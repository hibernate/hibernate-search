/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

/**
 * Build context for the worker and other backend
 * Available after all index, entity metadata are built.
 *
 * @author Emmanuel Bernard
 */
public interface WorkerBuildContext extends BuildContext {

	/**
	 * @return {@code true} if a transaction manager is expected, {@code false} otherwise.
	 *
	 * @see org.hibernate.search.cfg.spi.SearchConfiguration#isTransactionManagerExpected
	 */
	boolean isTransactionManagerExpected();

	/**
	 * @return {@code true} if it is safe to assume that the information we have about
	 * index metadata is accurate. This should be set to false for example if the index
	 * could contain Documents related to types not known to this SearchFactory instance.
	 * @see org.hibernate.search.cfg.spi.SearchConfiguration#isIndexMetadataComplete
	 */
	boolean isIndexMetadataComplete();

	/**
	 * @return {@code true} if regardless of {@code isIndexMetadataComplete} and the number
	 * of types present in the index it is safe to delete by term given that the underlying
	 * store guarantees uniqueness of ids
	 */
	boolean isDeleteByTermEnforced();

	/**
	 * @return An instance of the {@code InstanceInitializer} interface.
	 */
	InstanceInitializer getInstanceInitializer();

	/**
	 * @return {@code true} if the worker and the backend enlist their work in the current transaction;
	 * If {@code false}, the worker will still use the transaction as context but will execute the
	 * workload when the transaction commits.
	 */
	boolean enlistWorkerInTransaction();
}
