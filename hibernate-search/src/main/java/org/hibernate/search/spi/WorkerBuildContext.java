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
package org.hibernate.search.spi;

import java.util.Set;

import org.apache.lucene.search.Similarity;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.internals.DirectoryProviderData;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * Build context for the worker and other backend
 * Available after all index, entity metadata are built.
 *
 * @author Emmanuel Bernard
 */
public interface WorkerBuildContext extends BuildContext {
	/**
	 * Register the backend queue processor factory. Should only be called by the Worker implementation.
	 * TODO should we move it to a different interface
	 */
	void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory);

	OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider);

	Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> provider);

	LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> directoryProvider);

	Similarity getSimilarity(DirectoryProvider<?> directoryProvider);

	DirectoryProviderData getDirectoryProviderData(DirectoryProvider<?> provider);

	ErrorHandler getErrorHandler();

	<T> DocumentBuilderIndexedEntity<T> getDocumentBuilderIndexedEntity(Class<T> managedType);
}
