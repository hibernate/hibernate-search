/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.io.IOException;

import org.hibernate.search.test.TestResourceManager;

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

/**
 * Provides functionality useful for tests, e.g. for asserting the number of documents in a given index. Specific
 * implementations exist for different backends.
 *
 * @author Gunnar Morling
 */
public class BackendTestHelper {

	private final TestResourceManager resourceManager;

	private BackendTestHelper(TestResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	public static BackendTestHelper getInstance(TestResourceManager resourceManager) {
		return new BackendTestHelper( resourceManager );
	}

	public Directory openDirectoryForIndex(String indexName) {
		try {
			return FSDirectory.open( resourceManager.getBaseIndexDir().resolve( indexName ) );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Returns the number of indexed documents for the given index.
	 */
	public int getNumberOfDocumentsInIndex(String indexName) {
		try ( Directory directory = openDirectoryForIndex( indexName );
				IndexReader reader = DirectoryReader.open( directory ) ) {
			return reader.numDocs();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Returns the number of indexed documents for the given index satisfying the represented term or wildcard query.
	 */
	public int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		Term term = new Term( fieldName, value );
		Query query = value.contains( "*" ) ? new WildcardQuery( term ) : new TermQuery( term );

		try ( Directory directory = openDirectoryForIndex( indexName );
				IndexReader reader = DirectoryReader.open( directory ) ) {
			IndexSearcher searcher = new IndexSearcher( reader );
			TopDocs topDocs = searcher.search( query, 100 );
			return Math.toIntExact( topDocs.totalHits.value );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
