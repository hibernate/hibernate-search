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

import com.google.gson.Gson;


class ElasticsearchSourceProjectionBuilder implements SearchProjectionBuilder<String> {

	private final ElasticsearchSourceProjection projection;

	ElasticsearchSourceProjectionBuilder(Set<String> indexNames, Gson gson) {
		this.projection = new ElasticsearchSourceProjection( indexNames, gson );
	}

	@Override
	public SearchProjection<String> build() {
		return projection;
	}
}
