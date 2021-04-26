/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import javax.transaction.TransactionManager;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIndexingOptions;

public class HibernateOrmMassIndexingDocumentProducerInterceptor implements LoadingInterceptor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final SessionFactoryImplementor factory;
	private final TransactionManager transactionManager;
	private final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final HibernateOrmMassIndexingOptions options;

	public HibernateOrmMassIndexingDocumentProducerInterceptor(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmMassIndexingOptions options) {
		this.mappingContext = mappingContext;
		this.factory = mappingContext.sessionFactory();
		this.transactionManager = lookupTransactionManager( factory );
		this.transactionCoordinatorBuilder = lookupTransactionCoordinatorBuilder( factory );
		this.options = options;
	}

	private static TransactionCoordinatorBuilder lookupTransactionCoordinatorBuilder(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry().getService( TransactionCoordinatorBuilder.class );
	}

	private static TransactionManager lookupTransactionManager(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();
	}

	@Override
	public void intercept(LoadingInvocationContext ictx) throws Exception {
		CacheMode cacheMode = options.cacheMode();
		Integer transactionTimeout = options.transactionTimeout();
		String tenantId = options.tenantIdentifier();

		try ( SessionImplementor upperSession = (SessionImplementor) factory
				.withOptions()
				.tenantIdentifier( tenantId )
				.openSession() ) {
			upperSession.setHibernateFlushMode( FlushMode.MANUAL );
			upperSession.setCacheMode( cacheMode );
			upperSession.setDefaultReadOnly( true );

			HibernateOrmScopeSessionContext sessionContext = mappingContext.sessionContext( upperSession );
			ictx.context( PojoMassIndexingSessionContext.class, sessionContext );

			ictx.proceed( next -> {
				SessionImplementor session = sessionContext.session();
				ictx.context( SessionImplementor.class, session );
				beginTransaction( session, transactionTimeout );
				try {
					next.proceed();
					session.clear();
				}
				catch (Exception e) {
					try {
						rollbackTransaction( session );
					}
					catch (Exception e2) {
						e.addSuppressed( e2 );
					}
					throw e;
				}
				commitTransaction( session );
			} );
		}
	}

	private void beginTransaction(Session session, Integer transactionTimeout) {
		try {
			if ( transactionManager != null ) {
				if ( transactionTimeout != null ) {
					transactionManager.setTransactionTimeout( transactionTimeout );
				}
				transactionManager.begin();
			}
			else {
				session.beginTransaction();
			}
		}
		catch (Exception e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}

	private void commitTransaction(SessionImplementor session) {
		try {
			if ( transactionManager != null ) {
				transactionManager.commit();
			}
			else {
				session.accessTransaction().commit();
			}
		}
		catch (Exception e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}

	private void rollbackTransaction(SessionImplementor session) {
		try {
			if ( transactionManager != null ) {
				transactionManager.rollback();
			}
			else {
				session.accessTransaction().rollback();
			}
		}
		catch (Exception e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}

}
