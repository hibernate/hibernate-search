/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.apache.lucene.document.Document;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class DocumentReferenceProjectionHitExtractor extends AbstractDocumentReferenceHitExtractor<ProjectionHitCollector> {

	private static final DocumentReferenceProjectionHitExtractor INSTANCE = new DocumentReferenceProjectionHitExtractor();

	public static DocumentReferenceProjectionHitExtractor get() {
		return INSTANCE;
	}

	private DocumentReferenceProjectionHitExtractor() {
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document) {
		collector.collectProjection( extractDocumentReference( document ) );
	}
}
