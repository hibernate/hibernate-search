/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import com.google.gson.JsonObject;


class ElasticsearchExplanationProjectionBuilder implements SearchProjectionBuilder<JsonObject> {

	private final ElasticsearchExplanationProjection projection;

	ElasticsearchExplanationProjectionBuilder(Set<String> indexNames) {
		this.projection = new ElasticsearchExplanationProjection( indexNames );
	}

	@Override
	public SearchProjection<JsonObject> build() {
		return projection;
	}
}
