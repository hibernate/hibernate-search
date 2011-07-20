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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessorFactory;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.indexes.CommonPropertiesParse;
import org.hibernate.search.indexes.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.internals.DirectoryProviderData;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * First implementation will use the "legacy" DirectoryProvider which served us so well.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectoryBasedIndexManager implements IndexManager {
	
	private String indexName;
	private final DirectoryProvider directoryProvider;
	private Similarity similarity;
	private ExecutorService backendExecutor;
	private BackendQueueProcessorFactory backend;
	private OptimizerStrategy optimizer;
	private LuceneIndexingParameters inexingParameters;
	private Set<Class<?>> containedEntityTypes = new HashSet<Class<?>>();
	private final DirectoryProviderData directoryOptions = new DirectoryProviderData(); //TODO read these options out of properties
	
	public DirectoryBasedIndexManager(DirectoryProvider directoryProvider) {
		this.directoryProvider = directoryProvider;
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public IndexReader openReader() {
		return null;
	}

	@Override
	public void closeReader(IndexReader indexReader) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void initialize(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		this.indexName = indexName;
		backendExecutor = BackendFactory.buildWorkerExecutor( cfg, indexName );
		inexingParameters = CommonPropertiesParse.extractIndexingPerformanceOptions( cfg );
		optimizer = CommonPropertiesParse.getOptimizerStrategy( this, buildContext, cfg );
		backend = BackendFactory.createBackend( this, buildContext, cfg );
	}

	@Override
	public Set<Class<?>> getContainedTypes() {
		return containedEntityTypes;
	}

	@Override
	public Similarity getSimilarity() {
		return similarity;
	}

	@Override
	public void setSimilarity(Similarity newSimilarity) {
		this.similarity = newSimilarity;
	}

	public DirectoryProvider getDirectoryProvider() {
		return directoryProvider;
	}

	@Override
	public void performOperation(LuceneWork work) {
		ArrayList<LuceneWork> list = new ArrayList<LuceneWork>(1);
		list.add( work );
		Runnable runnable = backend.getProcessor( list );
		if ( backendExecutor != null ) {
			backendExecutor.execute( runnable );
		}
		else {
			runnable.run();
		}
	}

	@Override
	public DirectoryProviderData getDirectoryProviderData() {
		return directoryOptions;
	}

	@Override
	public OptimizerStrategy getOptimizerStrategy() {
		return optimizer;
	}

	@Override
	public LuceneIndexingParameters getIndexingParameters() {
		return inexingParameters;
	}

}
