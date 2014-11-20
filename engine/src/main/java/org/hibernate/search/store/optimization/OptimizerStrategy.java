/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.optimization;

import java.util.Properties;

import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.Workspace;

/**
 * Controls how and when the indexes are optimized.
 *
 * Note: Implementations need to be threadsafe.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface OptimizerStrategy {

	/**
	 * Invokes optimize on the IndexWriter; This is invoked when
	 * an optimization has been explicitly requested by the user API
	 * using {@link org.hibernate.search.spi.SearchIntegrator#optimize()} or {@link org.hibernate.search.spi.SearchIntegrator#optimize(Class)},
	 * or at the start or end of a MassIndexer's work.
	 *
	 * @param writer the index writer
	 * @return {@code true} if optimisation occurred, {@code false} otherwise
	 * @throws org.hibernate.search.exception.SearchException in case of IO errors on the index
	 */
	boolean performOptimization(IndexWriter writer);

	/**
	 * To count the amount of operations which where applied to the index.
	 * Invoked once per transaction.
	 *
	 * @param increment operation count
	 */
	void addOperationWithinTransactionCount(long increment);

	/**
	 * Allows the implementation to start an optimization process.
	 * The decision of optimizing or not is up to the implementor.
	 * This is invoked after all changes of a transaction are applied,
	 * but never during stream operation such as those used by
	 * the MassIndexer.
	 *
	 * @param workspace the current work space
	 */
	void optimize(Workspace workspace);

	/**
	 * Initializes the {@code OptimizerStrategy}. Is called once at the initialisation of the strategy.
	 *
	 * @param indexManager the index manager for which this strategy applies
	 * @param indexProperties the configuration properties
	 */
	void initialize(IndexManager indexManager, Properties indexProperties);
}
