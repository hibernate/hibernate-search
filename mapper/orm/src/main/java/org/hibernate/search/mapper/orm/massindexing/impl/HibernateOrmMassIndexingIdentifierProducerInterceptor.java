/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.intercepting.LoadingNextInvocation;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIndexingOptions;

public class HibernateOrmMassIndexingIdentifierProducerInterceptor implements LoadingInterceptor<HibernateOrmMassIndexingOptions> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final SessionFactoryImplementor factory;
	final TransactionManager transactionManager;
	final TransactionCoordinatorBuilder transactionCoordinatorBuilder;

	public HibernateOrmMassIndexingIdentifierProducerInterceptor(HibernateOrmMassIndexingMappingContext mappingContext) {
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
	public void intercept(LoadingInvocationContext<? extends HibernateOrmMassIndexingOptions> ictx) throws Exception {
		HibernateOrmMassIndexingOptions options = ictx.options();
		Integer transactionTimeout = options.transactionTimeout();
		String tenantId = options.tenantIdentifier();
		boolean wrapInTransaction = wrapInTransaction();
		if ( wrapInTransaction ) {
			try ( StatelessSession statelessSession = factory.withStatelessOptions()
					.tenantIdentifier( tenantId )
					.openStatelessSession() ) {
				if ( transactionTimeout != null ) {
					transactionManager.setTransactionTimeout( transactionTimeout );
				}
				transactionManager.begin();
				inTransactionWrapper( ictx, statelessSession, tenantId );
				transactionManager.commit();
			}
			// Just let runtime exceptions fall through
			catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException
					| HeuristicRollbackException e) {
				throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
			}
		}
		else {
			inTransactionWrapper( ictx, null, tenantId );
		}
	}

	private void inTransactionWrapper(LoadingInvocationContext<?> ictx, StatelessSession upperSession, String tenantId) throws Exception {
		StatelessSession session = upperSession;
		if ( upperSession == null ) {
			if ( tenantId == null ) {
				session = factory.openStatelessSession();
			}
			else {
				session = factory.withStatelessOptions().tenantIdentifier( tenantId ).openStatelessSession();
			}
		}
		try {
			SharedSessionContractImplementor sharedSession = (SharedSessionContractImplementor) session;
			Transaction transaction = sharedSession.accessTransaction();
			final boolean controlTransactions = !transaction.isActive();
			if ( controlTransactions ) {
				transaction.begin();
			}
			try {
				ictx.context( SharedSessionContractImplementor.class, sharedSession );
				ictx.proceed( LoadingNextInvocation::proceed );
			}
			finally {
				if ( controlTransactions ) {
					transaction.commit();
				}
			}
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
		finally {
			if ( upperSession == null ) {
				session.close();
			}
		}
	}

	boolean wrapInTransaction() {
		if ( !transactionCoordinatorBuilder.isJta() ) {
			//Today we only require a TransactionManager on JTA based transaction factories
			log.trace( "TransactionFactory does not require a TransactionManager: don't wrap in a JTA transaction" );
			return false;
		}
		if ( transactionManager == null ) {
			//no TM, nothing to do OR configuration mistake
			log.trace( "No TransactionManager found, do not start a surrounding JTA transaction" );
			return false;
		}
		try {
			if ( transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION ) {
				log.trace( "No Transaction in progress, needs to start a JTA transaction" );
				return true;
			}
		}
		catch (SystemException e) {
			log.cannotGuessTransactionStatus( e );
			return false;
		}
		log.trace( "Transaction in progress, no need to start a JTA transaction" );
		return false;
	}

}
