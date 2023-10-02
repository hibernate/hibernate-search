/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.io.IOException;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;

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
