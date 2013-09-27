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
package org.hibernate.search.indexes.spi;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Similarity;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * An IndexManager abstracts the specific configuration and implementations being used on a single index.
 * For each index a different implementation can be used, or different configurations.
 *
 * While in previous versions of Hibernate Search the backend could be sync or async, this fact is now
 * considered a detail of the concrete IndexManager implementations. This makes it possible to configure each index
 * manager (and hence index) differently. A concrete implementation can also decide to only support a specific mode
 * of operation. It can ignore some configuration properties or expect additional properties.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface IndexManager {

	/**
	 * Useful for labeling and logging resources from this instance.
	 *
	 * @return the name of the index maintained by this manager.
	 */
	String getIndexName();

	/**
	 * Provide access to {@code IndexReader}s.
	 *
	 * @return a {@code ReaderProvider} instance for the index managed by this instance
	 */
	ReaderProvider getReaderProvider();

	/**
	 * Used to apply update operations to the index.
	 * Operations can be applied in sync or async, depending on the IndexManager implementation and configuration.
	 *
	 * @param monitor no be notified of indexing events
	 * @param queue the list of write operations to apply.
	 */
	void performOperations(List<LuceneWork> queue, IndexingMonitor monitor);

	/**
	 * Perform a single non-transactional operation, best to stream large amounts of operations.
	 * Operations might be applied out-of-order! To mark two series of operations which need to be applied
	 * in order, use a transactional operation between them: a transactional operation will always flush
	 * all streaming operations first, and be applied before subsequent streaming operations.
	 *
	 * @param singleOperation the operation to perform
	 * @param monitor no be notified of indexing events
	 * @param forceAsync if true, the invocation will not block to wait for it being applied.
	 * When false this will depend on the backend configuration.
	 */
	void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync);

	/**
	 * Initialize this {@code IndexManager} before its use.
	 *
	 * @param indexName the unique name of the index (manager). Can be used to retrieve a {@code IndexManager} instance
	 * via the search factory and {@link org.hibernate.search.indexes.impl.IndexManagerHolder}.
	 * @param properties the configuration properties
	 * @param context context information needed to initialize this index manager
	 */
	void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context);

	/**
	 * Called when a {@code SearchFactory} is stopped. This method typically releases resources.
	 */
	void destroy();

	/**
	 * @return the set of classes being indexed in this manager
	 */
	Set<Class<?>> getContainedTypes();

	/**
	 * @return the {@code Similarity} applied to this index. Note, only a single {@code Similarity} can be applied to
	 *         a given index.
	 */
	Similarity getSimilarity();

	/**
	 * @param name the name of the analyzer to retrieve.
	 *
	 * @return Returns the {@code Analyzer} with the given name (see also {@link org.hibernate.search.annotations.AnalyzerDef})
	 * @throws org.hibernate.search.SearchException in case the analyzer name is unknown.
	 */
	Analyzer getAnalyzer(String name);

	/**
	 * Connects this {@code IndexManager} to a new {@code SearchFactory}.
	 *
	 * @param boundSearchFactory the existing search factory to which to associate this index manager with
	 */
	void setSearchFactory(SearchFactoryImplementor boundSearchFactory);

	/**
	 * @param entity Adds the specified entity type to this index manager, making it responsible for manging this type.
	 */
	void addContainedEntity(Class<?> entity);

	/**
	 * To optimize the underlying index. Some implementations might ignore the request, if it doesn't apply to them.
	 */
	void optimize();

	/**
	 * @return the Serializer implementation used for this IndexManager
	 */
	LuceneWorkSerializer getSerializer();

}
