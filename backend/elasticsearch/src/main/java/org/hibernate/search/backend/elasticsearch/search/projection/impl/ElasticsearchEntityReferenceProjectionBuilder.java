/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;


class ElasticsearchEntityReferenceProjectionBuilder<R> implements EntityReferenceProjectionBuilder<R> {

	private final ElasticsearchEntityReferenceProjection<R> projection;

	ElasticsearchEntityReferenceProjectionBuilder(Set<String> indexNames, DocumentReferenceExtractorHelper helper) {
		this.projection = new ElasticsearchEntityReferenceProjection<>( indexNames, helper );
	}

	@Override
	public SearchProjection<R> build() {
		return projection;
	}
}
