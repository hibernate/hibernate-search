/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.store.optimization.impl;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Optimization strategy triggered after a certain amount of operations
 *
 * @author Emmanuel Bernard
 */
public class IncrementalOptimizerStrategy implements OptimizerStrategy {
	
	private static final Log log = LoggerFactory.make();	

	private int operationMax = -1;
	private int transactionMax = -1;
	private long operations = 0;
	private long transactions = 0;
	private IndexManager indexManager;

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
			if ( log.isDebugEnabled() )
				log.debugv( "Optimize {0} after {1} operations and {2} transactions",
						indexManager.getIndexName(), operations, transactions );
			IndexWriter writer = workspace.getIndexWriter();
			try {
				writer.optimize();
			}
			catch (IOException e) {
				throw new SearchException( "Unable to optimize directoryProvider: "
						+ indexManager.getIndexName(), e );
			}
			optimizationForced();
		}
	}

	@Override
	public void initialize(IndexManager indexManager, Properties indexProperties) {
		this.indexManager = indexManager;
		operationMax = ConfigurationParseHelper.getIntValue( indexProperties, "optimizer.operation_limit.max", -1 );
		transactionMax = ConfigurationParseHelper.getIntValue( indexProperties, "optimizer.transaction_limit.max", -1 );
	}
}
