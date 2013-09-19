/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.metadata;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * @author Hardy Ferentschik
 */
class DummyIndexManager implements IndexManager {
	private final String indexName;

	public DummyIndexManager(String indexName) {
		this.indexName = indexName;
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public ReaderProvider getReaderProvider() {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public void performOperations(List<LuceneWork> queue, IndexingMonitor monitor) {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context) {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public void destroy() {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public Set<Class<?>> getContainedTypes() {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public Similarity getSimilarity() {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public void setSearchFactory(SearchFactoryImplementor boundSearchFactory) {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public void addContainedEntity(Class<?> entity) {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public void optimize() {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}

	@Override
	public LuceneWorkSerializer getSerializer() {
		throw new UnsupportedOperationException( "Not supported in dummy index manager" );
	}
}


