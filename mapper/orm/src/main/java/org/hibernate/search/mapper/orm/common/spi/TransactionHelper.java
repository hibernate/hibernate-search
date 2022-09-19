/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common.spi;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * A helper to abstract away all the complexity of starting transactions in different environments
 * (JTA/non-JTA in particular),
 * while accepting some JTA-specific settings (transaction timeout) on a best-effort basis.
 */
public final class TransactionHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final TransactionManager transactionManager;
	private final boolean useJta;
	private final Integer transactionTimeout;

	public TransactionHelper(SessionFactoryImplementor sessionFactory, Integer transactionTimeout) {
		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		transactionManager = HibernateOrmUtils.getServiceOrFail( serviceRegistry, JtaPlatform.class )
				.retrieveTransactionManager();
		TransactionCoordinatorBuilder transactionCoordinatorBuilder =
				HibernateOrmUtils.getServiceOrFail( serviceRegistry, TransactionCoordinatorBuilder.class );
		this.useJta = shouldUseJta( transactionManager, transactionCoordinatorBuilder );
		this.transactionTimeout = transactionTimeout;
	}

	public void inTransaction(SharedSessionContractImplementor session, Runnable action) {
		begin( session );
		try {
			action.run();
		}
		catch (Exception e) {
			rollbackSafely( session, e );
			throw e;
		}
		commit( session );
	}

	public <T> T inTransaction(SharedSessionContractImplementor session, Supplier<T> action) {
		begin( session );

		T result;
		try {
			result = action.get();
		}
		catch (Exception e) {
			rollbackSafely( session, e );
			throw e;
		}
		commit( session );
		return result;
	}

	public void begin(SharedSessionContractImplementor session) {
		try {
			if ( useJta ) {
				if ( transactionTimeout != null ) {
					transactionManager.setTransactionTimeout( transactionTimeout );
				}
				transactionManager.begin();
			}
			else {
				session.accessTransaction().begin();
			}
		}
		// Just let runtime exceptions fall through
		catch (NotSupportedException | SystemException e) {
			throw log.transactionHandlingException( e.getMessage(), e );
		}
	}

	public void commit(SharedSessionContractImplementor session) {
		try {
			if ( useJta ) {
				transactionManager.commit();
			}
			else {
				session.accessTransaction().commit();
			}
		}
		// Just let runtime exceptions fall through
		catch (SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
			throw log.transactionHandlingException( e.getMessage(), e );
		}
	}

	public void rollbackSafely(SharedSessionContractImplementor session, Throwable t) {
		try {
			rollback( session );
		}
		catch (RuntimeException e) {
			t.addSuppressed( e );
		}
	}

	private void rollback(SharedSessionContractImplementor session) {
		try {
			if ( useJta ) {
				transactionManager.rollback();
			}
			else {
				session.accessTransaction().rollback();
			}
		}
		catch (Exception e) {
			throw log.transactionHandlingException( e.getMessage(), e );
		}
	}

	private static boolean shouldUseJta(TransactionManager transactionManager,
			TransactionCoordinatorBuilder transactionCoordinatorBuilder) {
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
