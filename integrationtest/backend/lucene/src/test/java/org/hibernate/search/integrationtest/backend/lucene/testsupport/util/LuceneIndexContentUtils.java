/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.io.IOException;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.assertj.core.api.iterable.ThrowingExtractor;

public final class LuceneIndexContentUtils {

	private LuceneIndexContentUtils() {
	}

	// Reads directly from the index, without going through Hibernate Search
	public static <T> T readIndex(SearchSetupHelper setupHelper,
			String indexName, ThrowingExtractor<DirectoryReader, T, IOException> action)
			throws IOException {
		LuceneTckBackendAccessor accessor = (LuceneTckBackendAccessor) setupHelper.getBackendAccessor();
		try ( Directory directory = accessor.openDirectory( indexName );
				DirectoryReader reader = DirectoryReader.open( directory ) ) {
			return action.apply( reader );
		}
	}

	public static boolean indexExists(SearchSetupHelper setupHelper, String indexName) throws IOException {
		LuceneTckBackendAccessor accessor = (LuceneTckBackendAccessor) setupHelper.getBackendAccessor();
		try ( Directory directory = accessor.openDirectory( indexName ) ) {
			return DirectoryReader.indexExists( directory );
		}
	}
}
