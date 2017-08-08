/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import java.io.IOException;

import org.apache.lucene.search.TopDocs;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
class EmptyDocumentExtractor implements DocumentExtractor {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final DocumentExtractor INSTANCE = new EmptyDocumentExtractor();

	public static DocumentExtractor get() {
		return INSTANCE;
	}

	private EmptyDocumentExtractor() {
		// Use get()
	}

	@Override
	public EntityInfo extract(int index) throws IOException {
		throw new IndexOutOfBoundsException( "This document extractor is empty" );
	}

	@Override
	public int getFirstIndex() {
		return 0;
	}

	@Override
	public int getMaxIndex() {
		return -1;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public TopDocs getTopDocs() {
		throw log.documentExtractorTopDocsUnsupported();
	}
}
