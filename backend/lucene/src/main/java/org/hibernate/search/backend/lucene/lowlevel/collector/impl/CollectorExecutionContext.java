/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;

public final class CollectorExecutionContext {

	private final IndexReaderMetadataResolver metadataResolver;

	private final Map<String, NestedDocsProvider> nestedDocsProviders;

	private final int maxDocs;

	public CollectorExecutionContext(IndexReaderMetadataResolver metadataResolver,
			Map<String, NestedDocsProvider> nestedDocsProviders,
			int maxDocs) {
		this.metadataResolver = metadataResolver;
		this.nestedDocsProviders = nestedDocsProviders;
		this.maxDocs = maxDocs;
	}

	public IndexReaderMetadataResolver getMetadataResolver() {
		return metadataResolver;
	}

	public NestedDocsProvider getNestedDocsProvider(String nestedDocumentPath) {
		return nestedDocsProviders.get( nestedDocumentPath );
	}

	public int getMaxDocs() {
		return maxDocs;
	}
}
