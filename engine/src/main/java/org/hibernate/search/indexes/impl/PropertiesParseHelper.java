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
package org.hibernate.search.indexes.impl;

import java.util.Properties;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.store.optimization.impl.ExplicitOnlyOptimizerStrategy;
import org.hibernate.search.store.optimization.impl.IncrementalOptimizerStrategy;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * Contains helper to parse properties which should be read by the majority
 * of IndexManager implementations.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class PropertiesParseHelper {

	private PropertiesParseHelper() {
		// no need to create instances
	}

	/**
	 * <p>isExclusiveIndexUsageEnabled.</p>
	 *
	 * @param indexProps a {@link java.util.Properties} object.
	 */
	public static boolean isExclusiveIndexUsageEnabled(Properties indexProps) {
		return ConfigurationParseHelper.getBooleanValue( indexProps, Environment.EXCLUSIVE_INDEX_USE, true );
	}

	/**
	 * Returns the configured value of {@link org.hibernate.search.cfg.Environment#INDEX_METADATA_COMPLETE} for this specific index.
	 * If no value is set, the default is defined by {@link org.hibernate.search.cfg.spi.SearchConfiguration#isIndexMetadataComplete()}.
	 *
	 * @param indexProps The index configuration properties
	 * @param context The WorkerBuildContext provides a view of the default setting
	 *
	 * @return {@code true} when the index metadata is fully defined.
	 */
	public static boolean isIndexMetadataComplete(Properties indexProps, WorkerBuildContext context) {
		return ConfigurationParseHelper.getBooleanValue(
				indexProps,
				Environment.INDEX_METADATA_COMPLETE,
				context.isIndexMetadataComplete()
		);
	}

	/**
	 * @param indexName the index name (used for logging)
	 * @param indexProps MaskedProperties for this IndexManager
	 *
	 * @return the maximum queue length to be used on the backends of this index
	 */
	public static int extractMaxQueueSize(String indexName, Properties indexProps) {
		String maxQueueSize = indexProps.getProperty( Environment.MAX_QUEUE_LENGTH );
		if ( maxQueueSize != null ) {
			int parsedInt = ConfigurationParseHelper
					.parseInt(
							maxQueueSize, Executors.QUEUE_MAX_LENGTH,
							"Illegal value for property " + Environment.MAX_QUEUE_LENGTH + " on index " + indexName
					);
			if ( parsedInt < 1 ) {
				throw new SearchException(
						"Property " + Environment.MAX_QUEUE_LENGTH + " on index "
								+ indexName + "must be strictly positive"
				);
			}
			return parsedInt;
		}
		else {
			return Executors.QUEUE_MAX_LENGTH;
		}
	}

	public static OptimizerStrategy getOptimizerStrategy(IndexManager callback,
			Properties indexProperties,
			WorkerBuildContext buildContext) {
		MaskedProperty maskedProperty = new MaskedProperty( indexProperties, "optimizer" );
		String optimizerImplClassName = maskedProperty.getProperty( "implementation" );
		if ( optimizerImplClassName != null && ( !"default".equalsIgnoreCase( optimizerImplClassName ) ) ) {
			ServiceManager serviceManager = buildContext.getServiceManager();
			return ClassLoaderHelper.instanceFromName(
					OptimizerStrategy.class,
					optimizerImplClassName,
					"Optimizer Strategy",
					serviceManager
			);
		}
		else {
			boolean incremental = maskedProperty.containsKey( "operation_limit.max" )
					|| maskedProperty.containsKey( "transaction_limit.max" );
			OptimizerStrategy optimizerStrategy;
			if ( incremental ) {
				optimizerStrategy = new IncrementalOptimizerStrategy();
				optimizerStrategy.initialize( callback, maskedProperty );
			}
			else {
				optimizerStrategy = new ExplicitOnlyOptimizerStrategy();
			}
			return optimizerStrategy;
		}
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
	 * @param properties The properties extracted from the configuration.
	 */
	public static LuceneIndexingParameters extractIndexingPerformanceOptions(Properties properties) {
		return new LuceneIndexingParameters( properties );
	}

	public static DirectoryBasedReaderProvider createDirectoryBasedReaderProvider(DirectoryBasedIndexManager indexManager,
			Properties properties,
			WorkerBuildContext buildContext) {
		Properties maskedProperties = new MaskedProperty( properties, Environment.READER_PREFIX );
		String readerProviderImplName = maskedProperties.getProperty( "strategy" );
		DirectoryBasedReaderProvider readerProvider;
		if ( StringHelper.isEmpty( readerProviderImplName ) ) {
			readerProvider = new SharingBufferReaderProvider();
		}
		else if ( "not-shared".equalsIgnoreCase( readerProviderImplName ) ) {
			readerProvider = new NotSharedReaderProvider();
		}
		else if ( "shared".equalsIgnoreCase( readerProviderImplName ) ) {
			readerProvider = new SharingBufferReaderProvider();
		}
		else {
			ServiceManager serviceManager = buildContext.getServiceManager();
			readerProvider = ClassLoaderHelper.instanceFromName(
					DirectoryBasedReaderProvider.class,
					readerProviderImplName,
					"readerProvider",
					serviceManager
			);
		}
		readerProvider.initialize( indexManager, maskedProperties );
		return readerProvider;
	}

}
