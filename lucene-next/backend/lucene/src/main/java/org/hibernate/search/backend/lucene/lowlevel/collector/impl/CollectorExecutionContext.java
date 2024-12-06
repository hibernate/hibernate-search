/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;

import org.apache.lucene.search.IndexSearcher;

public class CollectorExecutionContext {

	private final IndexReaderMetadataResolver metadataResolver;

	private final IndexSearcher indexSearcher;

	private final int maxDocs;

	public CollectorExecutionContext(IndexReaderMetadataResolver metadataResolver,
			IndexSearcher indexSearcher,
			int maxDocs) {
		this.metadataResolver = metadataResolver;
		this.indexSearcher = indexSearcher;
		this.maxDocs = maxDocs;
	}

	public IndexReaderMetadataResolver getMetadataResolver() {
		return metadataResolver;
	}

	public IndexSearcher getIndexSearcher() {
		return indexSearcher;
	}

	public NestedDocsProvider createNestedDocsProvider(String parentDocumentPath, String nestedDocumentPath) {
		return new NestedDocsProvider( parentDocumentPath, nestedDocumentPath );
	}

	public NestedDocsProvider createNestedDocsProvider(Set<String> nestedDocumentPaths) {
		return new NestedDocsProvider( nestedDocumentPaths );
	}

	public int getMaxDocs() {
		return maxDocs;
	}
}
