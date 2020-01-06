/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public final class CollectorExecutionContext {

	private final IndexReaderMetadataResolver metadataResolver;

	private final IndexSearcher indexSearcher;

	private final Query luceneQuery;

	private final int maxDocs;

	public CollectorExecutionContext(IndexReaderMetadataResolver metadataResolver,
			IndexSearcher indexSearcher,
			Query luceneQuery,
			int maxDocs) {
		this.metadataResolver = metadataResolver;
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.maxDocs = maxDocs;
	}

	public IndexReaderMetadataResolver getMetadataResolver() {
		return metadataResolver;
	}

	public IndexSearcher getIndexSearcher() {
		return indexSearcher;
	}

	public NestedDocsProvider createNestedDocsProvider(String nestedDocumentPath) {
		return new NestedDocsProvider( nestedDocumentPath, luceneQuery );
	}

	public NestedDocsProvider createNestedDocsProvider(Set<String> nestedDocumentPaths) {
		return new NestedDocsProvider( nestedDocumentPaths, luceneQuery );
	}

	public int getMaxDocs() {
		return maxDocs;
	}
}
