/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.elasticsearch.query.impl.ElasticsearchHSQueryImpl;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.CustomTypeMetadata;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.SearchIntegrator;

import com.google.gson.JsonObject;

/**
 * A {@link QueryDescriptor} for an Elasticsearch query.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchJsonQueryDescriptor implements QueryDescriptor {

	/**
	 * The raw payload for the Search API, which will serve as a base
	 * to build the actual payload.
	 */
	private final JsonObject rawSearchPayload;

	public ElasticsearchJsonQueryDescriptor(JsonObject rawSearchPayload) {
		this.rawSearchPayload = rawSearchPayload;
	}

	@Override
	public HSQuery createHSQuery(SearchIntegrator integrator, IndexedTypeSet types) {
		return new ElasticsearchHSQueryImpl( rawSearchPayload, integrator.unwrap( ExtendedSearchIntegrator.class ), types );
	}

	@Override
	public HSQuery createHSQuery(SearchIntegrator integrator, IndexedTypeMap<CustomTypeMetadata> types) {
		return new ElasticsearchHSQueryImpl( rawSearchPayload, integrator.unwrap( ExtendedSearchIntegrator.class ), types );
	}

	@Override
	public String toString() {
		return rawSearchPayload.toString();
	}
}
