/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.optimization.impl;

import java.util.Properties;

import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Optimization strategy triggered after a certain amount of operations
 *
 * @author Emmanuel Bernard
 */
public class IncrementalOptimizerStrategy extends ExplicitOnlyOptimizerStrategy implements OptimizerStrategy {

	private static final Log log = LoggerFactory.make();

	private int operationMax = -1;
	private int transactionMax = -1;
	private volatile long operations = 0;
	private volatile long transactions = 0;
	private volatile long optimizationsPerformed = 0;
	private final Object lockOnCounters = new Object();

	@Override
	public boolean performOptimization(IndexWriter writer) {
		boolean done = super.performOptimization( writer );
		if ( done ) {
			synchronized ( lockOnCounters ) {
				operations = 0;
				transactions = 0;
				optimizationsPerformed++;
			}
		}
		return done;
	}

	private boolean needOptimization() {
		return (operationMax != -1 && operations >= operationMax)
				|| (transactionMax != -1 && transactions >= transactionMax);
	}

	@Override
	public void addOperationWithinTransactionCount(long operations) {
		synchronized ( lockOnCounters ) {
			this.operations += operations;
			this.transactions++;
		}
	}

	@Override
	public void optimize(Workspace workspace) {
		if ( needOptimization() ) {
			log.debugv( "Optimize {0} after {1} operations and {2} transactions",
						indexName, operations, transactions );
			IndexWriter writer = workspace.getIndexWriter();
			try {
				performOptimization( writer );
			}
			finally {
				workspace.afterTransactionApplied( false, false );
			}
		}
	}

	@Override
	public void initialize(IndexManager indexManager, Properties indexProperties) {
		super.initialize( indexManager, indexProperties );
		operationMax = ConfigurationParseHelper.getIntValue( indexProperties, "operation_limit.max", -1 );
		transactionMax = ConfigurationParseHelper.getIntValue( indexProperties, "transaction_limit.max", -1 );
	}

	public long getOptimizationsPerformed() {
		return optimizationsPerformed;
	}

}
