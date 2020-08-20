/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;

import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

/**
 * Default implementation of the {@link FacetManager} implementation.
 *
 * @author Hardy Ferentschik
 */
public class FacetManagerImpl implements FacetManager {
	/**
	 * The map of currently active/enabled facet requests.
	 */
	private Map<String, FacetingRequest> facetRequests;

	/**
	 * Keeps track of faceting results. This map gets populated once the query gets executed and needs to be
	 * reset on any query changing call.
	 */
	private Map<String, List<Facet>> facetResults;

	/**
	 * The query from which this manager was retrieved
	 */
	private final AbstractHSQuery query;

	public FacetManagerImpl(AbstractHSQuery query) {
		this.query = query;
	}

	@Override
	public FacetManager enableFaceting(FacetingRequest facetingRequest) {
		if ( facetRequests == null ) {
			facetRequests = newHashMap();
		}
		facetRequests.put( facetingRequest.getFacetingName(), (FacetingRequestImpl) facetingRequest );
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
		// if there are no facet requests we don't have to do anything
		if ( facetRequests == null || facetRequests.isEmpty() || !facetRequests.containsKey( facetingName ) ) {
			return Collections.emptyList();
		}

		List<Facet> facets = null;
		if ( facetResults != null ) {
			facets = facetResults.get( facetingName );
		}
		if ( facets != null ) {
			return facets;
		}
		query.extractFacetResults();
		//handle edge case of an empty index
		if ( facetResults == null ) {
			return Collections.emptyList();
		}
		List<Facet> results = facetResults.get( facetingName );
		if ( results != null ) {
			return results;
		}
		else {
			return Collections.emptyList();
		}
	}

	public Map<String, FacetingRequest> getFacetRequests() {
		return facetRequests != null ? facetRequests : Collections.<String, FacetingRequest>emptyMap();
	}

	public void setFacetResults(Map<String, List<Facet>> facetResults) {
		this.facetResults = facetResults;
	}

	void facetsHaveChanged() {
		this.facetResults = null;
		query.clearCachedResults();
	}

}


