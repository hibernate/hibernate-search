/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.metadata;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
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
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchFactory) {
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


