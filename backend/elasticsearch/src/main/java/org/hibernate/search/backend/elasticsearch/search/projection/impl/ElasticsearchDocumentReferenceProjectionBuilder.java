/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;


class ElasticsearchDocumentReferenceProjectionBuilder implements DocumentReferenceProjectionBuilder {

	private final ElasticsearchDocumentReferenceProjection projection;

	ElasticsearchDocumentReferenceProjectionBuilder(Set<String> indexNames, DocumentReferenceExtractionHelper helper) {
		this.projection = new ElasticsearchDocumentReferenceProjection( indexNames, helper );
	}

	@Override
	public SearchProjection<DocumentReference> build() {
		return projection;
	}
}
