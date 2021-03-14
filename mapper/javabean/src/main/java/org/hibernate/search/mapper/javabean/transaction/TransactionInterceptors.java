/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.transaction;

import java.lang.invoke.MethodHandles;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.transaction.impl.JavaBeanLoadingTransactionalContext;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;

public final class TransactionInterceptors {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private TransactionInterceptors() {
	}

	public static LoadingInterceptor<? extends TransactionOptions> withJTATransaction(TransactionManager tm) {
		return new JavaBeanTransactionInterceptor( new JavaBeanLoadingTransactionalContext( tm ), false );
	}

	public static LoadingInterceptor<? extends TransactionOptions> withJTAUncommittedTransaction(TransactionManager tm) {
		return new JavaBeanTransactionInterceptor( new JavaBeanLoadingTransactionalContext( tm ), true );
	}

	private static class JavaBeanTransactionInterceptor implements LoadingInterceptor<TransactionOptions> {

		private final JavaBeanLoadingTransactionalContext transactionalContext;
		private final boolean rolbackAlweys;

		public JavaBeanTransactionInterceptor(JavaBeanLoadingTransactionalContext transactionalContext, boolean rolbackAlweys) {
			this.transactionalContext = transactionalContext;
			this.rolbackAlweys = rolbackAlweys;
		}

		@Override
		public void intercept(LoadingInvocationContext<TransactionOptions> ictx) throws Exception {
			TransactionOptions options = ictx.options();
			boolean wrapInTransaction = transactionalContext.wrapInTransaction();
			Integer transactionTimeout = options.transactionTimeout();

			if ( wrapInTransaction ) {
				try {
					if ( transactionTimeout != null ) {
						transactionalContext.transactionManager().setTransactionTimeout( transactionTimeout );
					}
					ictx.active( transactionalContext::transactionInProgress );
					ictx.proceed( next -> {
						transactionalContext.transactionManager().begin();
						next.proceed();
						if ( rolbackAlweys ) {
							transactionalContext.transactionManager().rollback();

						}
						else {
							transactionalContext.transactionManager().commit();
						}
					} );
				}
				// Just let runtime exceptions fall through
				catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException
						| HeuristicRollbackException e) {
					throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
				}
				finally {
					rollback();
				}
			}
			else {
				ictx.proceed();
			}
		}

		private void rollback() {
			boolean wrapInTransaction = transactionalContext.wrapInTransaction();
			if ( wrapInTransaction ) {
				try {
					transactionalContext.transactionManager().rollback();
				}
				catch (SystemException e) {
					log.errorRollingBackTransaction( e.getMessage(), e );
				}
			}
		}
	}
}
