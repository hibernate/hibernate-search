/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.optimizer;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;

public class LeakingOptimizer implements OptimizerStrategy {

	private static final AtomicLong totalOperations = new AtomicLong();

	@Override
	public boolean performOptimization(IndexWriter writer) {
		return false;
	}

	@Override
	public void addOperationWithinTransactionCount(long increment) {
		totalOperations.addAndGet( increment );
	}

	@Override
	public void optimize(Workspace workspace) {
	}

	@Override
	public void initialize(IndexManager indexManager, Properties indexProperties) {
	}

	public static long getTotalOperations() {
		return totalOperations.get();
	}

	public static void reset() {
		totalOperations.set( 0l );
	}

}
