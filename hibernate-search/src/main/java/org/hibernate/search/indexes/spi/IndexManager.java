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
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * An IndexManager abstracts the specific configuration and implementations being used on a single Index.
 * For each index a different implementation can be used, or different configurations.
 * 
 * While in previous versions of Hibernate Search the backend could be sync or async, this is now
 * considered a detail of different IndexManager implementations, making it possible for them to be configured
 * in different ways, or to support only some modes of operation: configuration properties might be ignored
 * by some implementations, or look for additional properties.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface IndexManager {
	
	/**
	 * Useful for labeling and logging resources from this instance.
	 * @return the name of the index maintained by this manager.
	 */
	String getIndexName();
	
	/**
	 * Exposes a service to provide IndexReaders and close them
	 */
	ReaderProvider getIndexReaderManager();
	
	/**
	 * Used to apply update operations to the index.
	 * Operations can be applied in sync or async, depending on the IndexManager implementation and configuration.
	 * @param queue the list of write operations to apply.
	 */
	void performOperations(List<LuceneWork> queue);
	
	/**
	 * Perform a single non-transactional operation, best to stream large amounts of operations.
	 * Operations might be applied out-of-order! To mark two series of operations which need to be applied
	 * in order, use a transactional operation between them: a transactional operation will always flush
	 * all streaming operations first, and be applied before subsequent streaming operations.
	 * @param singleOperation
	 * @param forceAsync if true, the invocation will not block to wait for it being applied.
	 *  When false this will depend on the backend configuration.
	 */
	void performStreamOperation(LuceneWork singleOperation, boolean forceAsync);
	
	/**
	 * Initialize the IndexManager before its use.
	 */
	void initialize(String indexName, Properties props, WorkerBuildContext context);

	/**
	 * Called when a <code>SearchFactory</code> is stopped. This method typically releases resources.
	 */
	void destroy();

	/**
	 * @return the set of classes being indexed in this Index
	 */
	Set<Class<?>> getContainedTypes();

	/**
	 * Only a single Similarity can be applied to the same index, so this Similarity is applied to this index.
	 */
	Similarity getSimilarity();

	/**
	 * Not intended to be a mutable option, but the Similarity might be defined later in the boot lifecycle.
	 * @param newSimilarity
	 */
	void setSimilarity(Similarity newSimilarity);

	/**
	 * Returns the default Analyzer configured for this index.
	 */
	Analyzer getAnalyzer(String name);

	/**
	 * Connects this IndexManager to a new SearchFactory.
	 */
	void setSearchFactory(SearchFactoryImplementor boundSearchFactory);

	/**
	 * @param entity
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
