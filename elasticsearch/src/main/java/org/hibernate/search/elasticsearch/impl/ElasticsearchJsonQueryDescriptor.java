/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.SearchIntegrator;

import com.google.gson.JsonObject;

/**
 * A {@link QueryDescriptor} for an Elasticsearch query.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchJsonQueryDescriptor implements QueryDescriptor {

	private final JsonObject jsonQuery;

	public ElasticsearchJsonQueryDescriptor(JsonObject jsonQuery) {
		this.jsonQuery = jsonQuery;
	}

	@Override
	public HSQuery createHSQuery(SearchIntegrator searchIntegrator) {
		return new ElasticsearchHSQueryImpl( jsonQuery, searchIntegrator.unwrap( ExtendedSearchIntegrator.class ) );
	}

	@Override
	public String toString() {
		return jsonQuery.toString();
	}
}
