/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * Default implementation of the {@link FacetManager} implementation.
 *
 * @author Hardy Ferentschik
 */
public class FacetManagerImpl implements FacetManager {
	/**
	 * The map of currently active/enabled facet requests.
	 */
	private Map<String, FacetingRequestImpl<?>> facetRequests;

	/**
	 * Keeps track of faceting results. This map gets populated once the query gets executed and needs to be
	 * reset on any query changing call.
	 */
	private Map<String, List<Facet>> facetResults;

	/**
	 * The query from which this manager was retrieved
	 */
	private final HSQueryImpl<?> query;

	public FacetManagerImpl(HSQueryImpl<?> query) {
		this.query = query;
	}

	@Override
	public FacetManager enableFaceting(FacetingRequest facetingRequest) {
		if ( facetRequests == null ) {
			facetRequests = new HashMap<>();
		}
		facetRequests.put( facetingRequest.getFacetingName(), (FacetingRequestImpl<?>) facetingRequest );
		facetsHaveChanged();
		return this;
	}

	@Override
	public void disableFaceting(String facetingName) {
		if ( facetRequests != null ) {
			facetRequests.remove( facetingName );
		}
		if ( facetResults != null ) {
			facetResults.remove( facetingName );
		}
		facetsHaveChanged();
	}

	@Override
	public List<Facet> getFacets(String facetingName) {
		if ( !hasFacets() ) {
			return Collections.emptyList();
		}
		if ( facetResults == null ) {
			query.doFetch( 0, 0 );
		}
		List<Facet> facets = facetResults.get( facetingName );
		if ( facets == null ) {
			return Collections.emptyList();
		}
		return facets;
	}

	<LOS> SearchQueryOptionsStep<?, ?, LOS, ?, ?> contributeAggregations(SearchQueryOptionsStep<?, ?, LOS, ?, ?> optionsStep) {
		if ( !hasFacets() ) {
			return optionsStep;
		}
		for ( FacetingRequestImpl<?> facetRequest : facetRequests.values() ) {
			optionsStep = requestAggregation( optionsStep, facetRequest );
		}
		return optionsStep;
	}

	void setFacetResults(SearchResult<?> result) {
		if ( !hasFacets() ) {
			return;
		}
		this.facetResults = new HashMap<>();
		for ( Map.Entry<String, FacetingRequestImpl<?>> entry : facetRequests.entrySet() ) {
			List<Facet> facets = extractFacets( result, entry.getValue() );
			this.facetResults.put( entry.getKey(), facets );
		}
	}

	private boolean hasFacets() {
		return facetRequests != null && !facetRequests.isEmpty();
	}

	private <LOS, A> SearchQueryOptionsStep<?, ?, LOS, ?, ?> requestAggregation(
			SearchQueryOptionsStep<?, ?, LOS, ?, ?> optionsStep,
			FacetingRequestImpl<A> facetRequest) {
		return optionsStep.aggregation( facetRequest.getKey(), facetRequest::requestAggregation );
	}

	private <A> List<Facet> extractFacets(SearchResult<?> result, FacetingRequestImpl<A> facetRequest) {
		A aggregation = result.aggregation( facetRequest.getKey() );
		return facetRequest.toFacets( aggregation );
	}

	private void facetsHaveChanged() {
		this.facetResults = null;
	}
}

