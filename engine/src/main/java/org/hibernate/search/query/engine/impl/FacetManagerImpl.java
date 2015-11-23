/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetCombine;
import org.hibernate.search.query.facet.FacetSelection;
import org.hibernate.search.query.facet.FacetingRequest;

import static org.hibernate.search.util.impl.CollectionHelper.newArrayList;
import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

/**
 * Default implementation of the {@link org.hibernate.search.query.engine.spi.FacetManager} implementation.
 *
 * @author Hardy Ferentschik
 */
public class FacetManagerImpl implements FacetManager {
	/**
	 * The map of currently active/enabled facet requests.
	 */
	private Map<String, FacetingRequest> facetRequests;

	/**
	 * Keep track of the current facet selection groups.
	 */
	private Map<String, FacetSelectionImpl> facetSelection;

	/**
	 * Keeps track of faceting results. This map gets populated once the query gets executed and needs to be
	 * reset on any query changing call.
	 */
	private Map<String, List<Facet>> facetResults;

	/**
	 * The set of boolean clause filters for all selected facets which needs to be applied on the current query
	 */
	private QueryFilters facetFilterset;

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
		queryHasChanged();
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
		queryHasChanged();
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

	@Override
	public FacetSelection getFacetGroup(String groupName) {
		if ( groupName == null ) {
			throw new IllegalArgumentException( "null is not a valid facet selection group name" );
		}
		if ( facetSelection == null ) {
			facetSelection = newHashMap();
		}
		FacetSelectionImpl selection = facetSelection.get( groupName );
		if ( selection == null ) {
			selection = new FacetSelectionImpl();
			facetSelection.put( groupName, selection );
		}
		return selection;
	}

	public Map<String, FacetingRequest> getFacetRequests() {
		return facetRequests != null ? facetRequests : Collections.<String, FacetingRequest>emptyMap();
	}

	public void setFacetResults(Map<String, List<Facet>> facetResults) {
		this.facetResults = facetResults;
	}

	void queryHasChanged() {
		facetFilterset = null;
		this.facetResults = null;
		query.clearCachedResults();
	}

	public QueryFilters getFacetFilters() {
		if ( facetFilterset == null ) {
			int size = facetSelection == null ? 0 : facetSelection.values().size();
			if ( size != 0 ) {
				List<Query> filterQueries = new ArrayList<>( size );
				for ( FacetSelectionImpl selection : facetSelection.values() ) {
					if ( !selection.getFacetList().isEmpty() ) {
						Query selectionGroupQuery = createSelectionGroupQuery( selection );
						filterQueries.add( selectionGroupQuery );
					}
				}
				if ( filterQueries.size() != 0 ) {
					this.facetFilterset = new QueryFilters( filterQueries );
				}
				else {
					facetFilterset = QueryFilters.EMPTY_FILTERSET;
				}
			}
			else {
				facetFilterset = QueryFilters.EMPTY_FILTERSET;
			}
		}
		return facetFilterset;
	}

	private Query createSelectionGroupQuery(FacetSelectionImpl selection) {
		BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
		for ( Facet facet : selection.getFacetList() ) {
			boolQueryBuilder.add( facet.getFacetQuery(), selection.getOccurType() );
		}
		return boolQueryBuilder.build();
	}

	class FacetSelectionImpl implements FacetSelection {
		private final List<Facet> facetList = newArrayList();

		private BooleanClause.Occur occurType = BooleanClause.Occur.SHOULD;

		public List<Facet> getFacetList() {
			return facetList;
		}

		@Override
		public void selectFacets(Facet... facets) {
			selectFacets( FacetCombine.OR, facets );
		}

		@Override
		public void selectFacets(FacetCombine combineBy, Facet... facets) {
			if ( facets == null ) {
				return;
			}

			if ( FacetCombine.OR.equals( combineBy ) ) {
				occurType = BooleanClause.Occur.SHOULD;
			}
			else {
				occurType = BooleanClause.Occur.MUST;
			}

			facetList.addAll( Arrays.asList( facets ) );
			queryHasChanged();
		}

		@Override
		public List<Facet> getSelectedFacets() {
			return Collections.unmodifiableList( facetList );
		}

		@Override
		public void deselectFacets(Facet... facets) {
			boolean hasChanged = facetList.removeAll( Arrays.asList( facets ) );
			if ( hasChanged ) {
				queryHasChanged();
			}
		}

		@Override
		public void clearSelectedFacets() {
			facetList.clear();
			queryHasChanged();
		}

		@Override
		public BooleanClause.Occur getOccurType() {
			return occurType;
		}
	}
}


