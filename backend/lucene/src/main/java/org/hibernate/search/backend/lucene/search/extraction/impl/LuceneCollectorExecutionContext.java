/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;

public final class LuceneCollectorExecutionContext {

	private final IndexReaderMetadataResolver metadataResolver;

	private final int maxDocs;

	LuceneCollectorExecutionContext(IndexReaderMetadataResolver metadataResolver, int maxDocs) {
		this.metadataResolver = metadataResolver;
		this.maxDocs = maxDocs;
	}

	public IndexReaderMetadataResolver getMetadataResolver() {
		return metadataResolver;
	}

	public int getMaxDocs() {
		return maxDocs;
	}
}
