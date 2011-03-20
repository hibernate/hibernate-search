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
package org.hibernate.search.engine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.search.Similarity;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.stat.StatisticsImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * Interface which gives access to the metadata. Intended to be used by Search components
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface SearchFactoryImplementor extends SearchFactoryIntegrator {
	BackendQueueProcessorFactory getBackendQueueProcessorFactory();

	Map<Class<?>, DocumentBuilderIndexedEntity<?>> getDocumentBuildersIndexedEntities();

	<T> DocumentBuilderIndexedEntity<T> getDocumentBuilderIndexedEntity(Class<T> entityType);

	<T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType);

	OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider);

	FilterCachingStrategy getFilterCachingStrategy();

	FilterDef getFilterDefinition(String name);

	LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider);

	String getIndexingStrategy();

	Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> directoryProvider);

	Set<DirectoryProvider<?>> getDirectoryProviders();

	ReentrantLock getDirectoryProviderLock(DirectoryProvider<?> dp);

	int getFilterCacheBitResultsSize();

	Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes);

	BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor, Integer writerThreads);

	Similarity getSimilarity(DirectoryProvider<?> directoryProvider);

	ErrorHandler getErrorHandler();

	boolean isJMXEnabled();

	/**
	 * Retrieve the statistics implementor instance for this factory.
	 *
	 * @return The statistics implementor.
	 */
	public StatisticsImplementor getStatisticsImplementor();

	/**
	 * @return true if we are allowed to inspect entity state to
	 * potentially skip some indexing operations.
	 * Can be disabled to get pre-3.4 behavior (always rebuild document)
	 */
	boolean isDirtyChecksEnabled();

}
