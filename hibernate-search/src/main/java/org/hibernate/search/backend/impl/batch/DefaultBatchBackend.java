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
package org.hibernate.search.backend.impl.batch;

import java.util.Properties;

import org.hibernate.search.Environment;
import org.hibernate.search.engine.spi.EntityIndexMapping;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.StreamingSelectionVisitor;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This is not meant to be used as a regular
 * backend, only to apply batch changes to the index. Several threads
 * are used to make changes to each index, so order of Work processing is not guaranteed.
 * 
 * @author Sanne Grinovero
 * @experimental First {@code BatchBackend}
 */
public class DefaultBatchBackend implements BatchBackend {
	
	public static final String CONCURRENT_WRITERS = Environment.BATCH_BACKEND + ".concurrent_writers";

	private static final StreamingSelectionVisitor providerSelectionVisitor = new StreamingSelectionVisitor();

	private SearchFactoryIntegrator searchFactoryImplementor;

	public void initialize(Properties cfg, MassIndexerProgressMonitor monitor, SearchFactoryIntegrator searchFactory) {
		this.searchFactoryImplementor = searchFactory;
	}

	public void enqueueAsyncWork(LuceneWork work) throws InterruptedException {
		sendWorkToShards( work, true );
	}

	public void doWorkInSync(LuceneWork work) {
		sendWorkToShards( work, false );
	}
	
	private void sendWorkToShards(LuceneWork work, boolean forceAsync) {
		final Class<?> entityType = work.getEntityClass();
		EntityIndexMapping<?> entityIndexMapping = searchFactoryImplementor.getIndexMappingForEntity( entityType );
		IndexShardingStrategy shardingStrategy = entityIndexMapping.getSelectionStrategy();
		work.getWorkDelegate( providerSelectionVisitor ).performStreamOperation( work, shardingStrategy, forceAsync );
	}

}
