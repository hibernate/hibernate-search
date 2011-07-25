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

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.engine.spi.EntityIndexMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * An IndexManager abstracts the specific configuration and implementations being used on a single Index.
 * For each index a different implementation can be used, or different configurations.
 * 
 * While in previous versions of Hibernate Search the backend could be sync or async, this is now
 * considered a detail of different IndexManager implementations, making it possible for them to be configured
 * in different ways, or to support only some modes of operation.
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
	 * Provides an IndexReader instance which is guaranteed to provide a fresh read view
	 * on the index.
	 * @return a read-only IndexReader instance.
	 */
	IndexReader openReader();
	
	/**
	 * Used to apply update operations to the index.
	 * Operations can be applied in sync or async, depending on the IndexManager implementation and configuration.
	 * @param queue the list of write operations to apply.
	 */
	void performOperation(List<LuceneWork> work);
	
	/**
	 * Perform a single non-transactional operation, best to stream large amounts of operations.
	 * Operations might be applied out-of-order! To mark two series of operations which need to be applied
	 * in order, use a transactional operation between them: a transactional operation will always flush
	 * all streaming operations first, and be applied before subsequent streaming operations.
	 * @param singleOperation
	 */
	void performStreamOperation(LuceneWork singleOperation);
	
	/**
	 * Initialize the reader provider before its use.
	 */
	void initialize(String indexName, Properties props, WorkerBuildContext context);

	/**
	 * Called when a <code>SearchFactory</code> is destroyed. This method typically releases resources.
	 * It is guaranteed to be executed after readers are released by queries (assuming no user error). 
	 */
	void destroy();

	/**
	 * @return the set of classes being indexed in this Index
	 */
	Set<Class<?>> getContainedTypes();

	/**
	 * 
	 */
	Similarity getSimilarity();

	/**
	 * @param newSimilarity
	 */
	void setSimilarity(Similarity newSimilarity);

	/**
	 * @return
	 */
	OptimizerStrategy getOptimizerStrategy();

	/**
	 * @return
	 */
	LuceneIndexingParameters getIndexingParameters();

	/**
	 * @return
	 */
	ErrorHandler getErrorHandler();
	
	EntityIndexMapping<?> getIndexMappingForEntity(Class<?> type);
	
	Analyzer getAnalyzer(String name);

	/**
	 * @param boundSearchFactory
	 */
	void setBoundSearchFactory(SearchFactoryImplementor boundSearchFactory);

	/**
	 * @param entity
	 */
	void addContainedEntity(Class<?> entity);

	/**
	 * 
	 */
	void optimize();
	
}
