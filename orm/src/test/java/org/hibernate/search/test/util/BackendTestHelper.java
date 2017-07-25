/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.test.TestResourceManager;

/**
 * Provides functionality useful for tests, e.g. for asserting the number of documents in a given index. Specific
 * implementations exist for different backends.
 *
 * @author Gunnar Morling
 */
public abstract class BackendTestHelper {

	private static final String ELASTIC_SEARCH_TEST_HELPER_CLASS_NAME = "org.hibernate.search.elasticsearch.testutil.ElasticsearchBackendTestHelper";

	protected BackendTestHelper() {
	}

	public static BackendTestHelper getInstance(TestResourceManager resourceManager) {
		BackendTestHelper instance;

		try {
			Class<?> clazz = Class.forName( ELASTIC_SEARCH_TEST_HELPER_CLASS_NAME );
			instance = (BackendTestHelper) clazz.getConstructor( TestResourceManager.class ).newInstance( resourceManager );
		}
		catch (Exception e) {
			// ES backend not present, use Lucene-based helper by default
			instance = new LuceneBackendTestHelper( resourceManager );
		}

		return instance;
	}

	/**
	 * Returns the number of indexed documents for the given type.
	 */
	public abstract int getNumberOfDocumentsInIndex(IndexedTypeIdentifier entityType);

	/**
	 * Returns the number of indexed documents for the given index.
	 */
	public abstract int getNumberOfDocumentsInIndex(String indexName);

	/**
	 * Returns the number of indexed documents for the given index satisfying the represented term or wildcard query.
	 */
	public abstract int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value);


	private static class LuceneBackendTestHelper extends BackendTestHelper {

		private final TestResourceManager resourceManager;

		public LuceneBackendTestHelper(TestResourceManager resourceManager) {
			this.resourceManager = resourceManager;
		}

		public Directory getDirectory(IndexedTypeIdentifier entityType) {
			ExtendedSearchIntegrator integrator = resourceManager.getExtendedSearchIntegrator();
			Set<IndexManager> indexManagers = integrator.getIndexBinding( entityType ).getIndexManagerSelector().all();
			DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers.iterator().next();
			return indexManager.getDirectoryProvider().getDirectory();
		}

		@Override
		public int getNumberOfDocumentsInIndex(IndexedTypeIdentifier entityType) {
			try ( IndexReader reader = DirectoryReader.open( getDirectory( entityType ) ) ) {
				return reader.numDocs();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		@Override
		public int getNumberOfDocumentsInIndex(String indexName) {
			try (
					// TODO When using IndexReaderAccessor as below, ShardsTest fails; It seems to not know about
					// the custom shard identifier configured
					FSDirectory directory = FSDirectory.open( resourceManager.getBaseIndexDir().resolve( indexName ) );
					IndexReader reader = DirectoryReader.open( directory ) ) {
				return reader.numDocs();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		@Override
		public int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
			IndexReaderAccessor indexReaderAccessor = resourceManager.getExtendedSearchIntegrator().getIndexReaderAccessor();
			Term term = new Term( fieldName, value );
			Query query = value.contains( "*" ) ? new WildcardQuery( term ) : new TermQuery( term );

			try ( IndexReader reader = indexReaderAccessor.open( indexName ) ) {
				IndexSearcher searcher = new IndexSearcher( reader );
				TopDocs topDocs = searcher.search( query, 100 );
				return topDocs.totalHits;
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
	}
}
