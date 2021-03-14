/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
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
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIndexingOptions;

public class HibernateOrmMassIndexingDocumentProducerInterceptor implements LoadingInterceptor<HibernateOrmMassIndexingOptions> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final SessionFactoryImplementor factory;
	final TransactionManager transactionManager;
	final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final HibernateOrmMassIndexingMappingContext mappingContext;

	public HibernateOrmMassIndexingDocumentProducerInterceptor(HibernateOrmMassIndexingMappingContext mappingContext) {
		this.mappingContext = mappingContext;
		this.factory = mappingContext.sessionFactory();
		this.transactionManager = lookupTransactionManager( factory );
		this.transactionCoordinatorBuilder = lookupTransactionCoordinatorBuilder( factory );
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
	public void intercept(LoadingInvocationContext<HibernateOrmMassIndexingOptions> ictx) throws Exception {
		HibernateOrmMassIndexingOptions indexer = ictx.options();
		CacheMode cacheMode = indexer.cacheMode();
		Integer transactionTimeout = indexer.transactionTimeout();
		String tenantId = ictx.tenantId();

		try ( SessionImplementor upperSession = (SessionImplementor) factory
				.withOptions()
				.tenantIdentifier( tenantId )
				.openSession() ) {
			upperSession.setHibernateFlushMode( FlushMode.MANUAL );
			upperSession.setCacheMode( cacheMode );
			upperSession.setDefaultReadOnly( true );

			HibernateOrmScopeSessionContext sessionContext = mappingContext.sessionContext( upperSession );
			ictx.contextData().put( MassIndexingSessionContext.class, sessionContext );

			ictx.proceed( next -> {
				SessionImplementor session = sessionContext.session();
				ictx.contextData().put( SessionImplementor.class, session );
				try {
					beginTransaction( session, transactionTimeout );
					next.proceed();
					session.clear();
				}
				finally {
					// it's read-only, so no need to commit
					rollbackTransaction( session );
				}

			} );

		}
	}

	private void beginTransaction(Session session, Integer transactionTimeout) throws SystemException, NotSupportedException {
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

	private void rollbackTransaction(SessionImplementor session) {
		try {
			if ( transactionManager != null ) {
				transactionManager.rollback();
			}
			else {
				session.accessTransaction().rollback();
			}
		}
		catch (IllegalStateException | SecurityException | SystemException e) {
			log.errorRollingBackTransaction( e.getMessage(), e );
		}
	}

}
