/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;

class ObjectHitExtractor extends AbstractDocumentReferenceHitExtractor<LoadingHitCollector> {

	private static final ObjectHitExtractor INSTANCE = new ObjectHitExtractor();

	public static ObjectHitExtractor get() {
		return INSTANCE;
	}

	private ObjectHitExtractor() {
	}

	@Override
	public void extract(LoadingHitCollector collector, IndexSearcher indexSearcher, ScoreDoc scoreDoc) throws IOException {
		collector.collectForLoading( extractDocumentReference( indexSearcher, scoreDoc.doc ) );
	}
}
