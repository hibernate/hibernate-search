// $Id$
package org.hibernate.search.store.optimization;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.LoggerFactory;

/**
 * Optimization strategy triggered after a certain amount of operations
 *
 * @author Emmanuel Bernard
 */
public class IncrementalOptimizerStrategy implements OptimizerStrategy {
	
	private static final Logger log = LoggerFactory.make();	

	private int operationMax = -1;
	private int transactionMax = -1;
	private long operations = 0;
	private long transactions = 0;
	private DirectoryProvider directoryProvider;

	public void initialize(DirectoryProvider directoryProvider, Properties indexProperties, SearchFactoryImplementor searchFactoryImplementor) {
		this.directoryProvider = directoryProvider;
		operationMax = ConfigurationParseHelper.getIntValue( indexProperties, "optimizer.operation_limit.max", -1 );
		transactionMax = ConfigurationParseHelper.getIntValue( indexProperties, "optimizer.transaction_limit.max", -1 );
	}

	public void optimizationForced() {
		operations = 0;
		transactions = 0;
	}

	public boolean needOptimization() {
		return (operationMax != -1 && operations >= operationMax)
				|| (transactionMax != -1 && transactions >= transactionMax);
	}

	public void addTransaction(long operations) {
		this.operations += operations;
		this.transactions++;
	}

	public void optimize(Workspace workspace) {
		if ( needOptimization() ) {
			log.debug( "Optimize {} after {} operations and {} transactions",
				new Object[] { directoryProvider.getDirectory(), operations, transactions });
			IndexWriter writer = workspace.getIndexWriter( false ); //TODO true or false?
			try {
				writer.optimize();
			}
			catch (IOException e) {
				throw new SearchException( "Unable to optimize directoryProvider: "
						+ directoryProvider.getDirectory().toString(), e );
			}
			optimizationForced();
		}
	}
}
