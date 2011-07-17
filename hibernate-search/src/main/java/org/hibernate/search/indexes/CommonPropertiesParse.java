/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes;

import java.util.Properties;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.spi.WritableBuildContext;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.store.optimization.impl.IncrementalOptimizerStrategy;
import org.hibernate.search.store.optimization.impl.NoOpOptimizerStrategy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * Contains helper to parse properties which should be read by the majority
 * of IndexManager implementations.
 *  
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CommonPropertiesParse {
	
	private CommonPropertiesParse() {
		// no need to create instances
	}
	
	public static boolean isExclusiveIndexUsageEnabled(String indexName, Properties indexProps) {
		String exclusiveIndexUsageProperty = indexProps.getProperty( Environment.EXCLUSIVE_INDEX_USE, "false" );
		boolean exclusiveIndexUsage = ConfigurationParseHelper.parseBoolean(
				exclusiveIndexUsageProperty,
				"Illegal value for property " + Environment.EXCLUSIVE_INDEX_USE + " on index " + indexName
		);
		return exclusiveIndexUsage;
	}

	/**
	 * @param indexName
	 * @param indexProps MaskedProperties for this DirectoryProvider
	 * @return the maximum queue length to be used on the backends of this index
	 */
	public static int extractMaxQueueSize(String indexName, Properties indexProps) {
		String maxQueueSize = indexProps.getProperty( Environment.MAX_QUEUE_LENGTH );
		if ( maxQueueSize != null ) {
			int parsedInt= ConfigurationParseHelper
					.parseInt(
							maxQueueSize, Executors.QUEUE_MAX_LENGTH,
							"Illegal value for property " + Environment.MAX_QUEUE_LENGTH + " on index " + indexName
					);
			if ( parsedInt < 1 ) {
				throw new SearchException( "Property " + Environment.MAX_QUEUE_LENGTH + " on index "
						+ indexName + "must be strictly positive" );
			}
			return parsedInt;
		}
		else {
			return Executors.QUEUE_MAX_LENGTH;
		}
	}

	public static OptimizerStrategy getOptimizerStrategy(WritableBuildContext context, Properties indexProps, IndexManager callback) {
		boolean incremental = indexProps.containsKey( "optimizer.operation_limit.max" )
				|| indexProps.containsKey( "optimizer.transaction_limit.max" );
		OptimizerStrategy optimizerStrategy;
		if ( incremental ) {
			optimizerStrategy = new IncrementalOptimizerStrategy();
			optimizerStrategy.initialize( callback, indexProps );
		}
		else {
			optimizerStrategy = new NoOpOptimizerStrategy();
		}
		return optimizerStrategy;
	}
	
	/**
	 * Creates a new <code>LuceneIndexingParameters</code> instance for the specified provider.
	 * If there are no matching properties in the configuration default values will be applied.
	 * <p>
	 * NOTE:</br>
	 * If a non batch value is set in the configuration apply it also to the
	 * batch mode. This covers the case where users only specify
	 * parameters for the non batch mode. In this case the same parameters apply for
	 * batch indexing. Parameters are found "depth-first": if a batch parameter is set
	 * in a global scope it will take priority on local transaction parameters.
	 * </p>
	 *
	 * @param context the build context.
	 * @param directoryProperties The properties extracted from the configuration.
	 * @param provider The directory provider for which to configure the indexing parameters.
	 */
	public static LuceneIndexingParameters extractIndexingPerformanceOptions(Properties indexProps) {
		LuceneIndexingParameters indexingParams = new LuceneIndexingParameters( indexProps );
		return indexingParams;
	}

}
