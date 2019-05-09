/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;


class ElasticsearchEntityProjectionBuilder<O> implements EntityProjectionBuilder<O> {

	private final ElasticsearchEntityProjection<O> projection;

	ElasticsearchEntityProjectionBuilder(DocumentReferenceExtractorHelper helper) {
		this.projection = new ElasticsearchEntityProjection<>( helper );
	}

	@Override
	public SearchProjection<O> build() {
		return projection;
	}
}
