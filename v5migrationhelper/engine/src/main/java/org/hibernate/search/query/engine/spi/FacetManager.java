/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.engine.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * Interface defining methods around faceting.
 *
 * @author Hardy Ferentschik
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@code org.hibernate.search.mapper.orm.session.SearchSession}
 * using {@code org.hibernate.search.mapper.orm.Search#session(org.hibernate.Session)},
 * create a {@link SearchQuery} with {@code org.hibernate.search.mapper.orm.session.SearchSession#search(Class)},
 * and define your facets (now called aggregations)
 * using {@link SearchQueryOptionsStep#aggregation(AggregationKey, Function)}.
 * You can then fetch the query result using {@link SearchQuery#fetch(Integer)}
 * and get each aggregation using {@link SearchResult#aggregation(AggregationKey)}.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
public interface FacetManager {
	/**
	 * Enable a facet request.
	 *
	 * @param facetingRequest the faceting request
	 * @return {@code this} to allow method chaining
	 */
	FacetManager enableFaceting(FacetingRequest facetingRequest);

	/**
	 * Disable a facet with the given name.
	 *
	 * @param facetingName the name of the facet to disable.
	 */
	void disableFaceting(String facetingName);

	/**
	 * Returns the {@code Facet}s for a given facet name
	 *
	 * @param facetingName the facet name for which to return the facet list
	 * @return the facet result list which corresponds to the facet request with the given name. The empty list
	 *         is returned for an unknown facet name.
	 * @see #enableFaceting(FacetingRequest)
	 */
	List<Facet> getFacets(String facetingName);
}
