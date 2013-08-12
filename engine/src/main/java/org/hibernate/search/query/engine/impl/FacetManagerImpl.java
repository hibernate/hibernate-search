/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.query.engine.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
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
	private final Map<String, FacetingRequestImpl> facetRequests = newHashMap();

	/**
	 * Keep track of the current facet selection groups.
	 */
	private final Map<String, FacetSelectionImpl> facetSelection = newHashMap();

	/**
	 * Keeps track of faceting results. This map gets populated once the query gets executed and needs to be
	 * reset on any query changing call.
	 */
	private Map<String, List<Facet>> facetResults;

	/**
	 * The combined filter for all selected facets which needs to be applied on the current query
	 */
	private Filter facetFilter;

	/**
	 * The query from which this manager was retrieved
	 */
	private final HSQueryImpl query;

	FacetManagerImpl(HSQueryImpl query) {
		this.query = query;
	}

	@Override
	public FacetManager enableFaceting(FacetingRequest facetingRequest) {
		facetRequests.put( facetingRequest.getFacetingName(), (FacetingRequestImpl) facetingRequest );
		queryHasChanged();
		return this;
	}

	@Override
	public void disableFaceting(String facetingName) {
		facetRequests.remove( facetingName );
		if ( facetResults != null ) {
			facetResults.remove( facetingName );
		}
		queryHasChanged();
	}

	@Override
	public List<Facet> getFacets(String facetingName) {
		// if there are no facet requests we don't have to do anything
		if ( facetRequests.isEmpty() || !facetRequests.containsKey( facetingName ) ) {
			return Collections.emptyList();
		}

		List<Facet> facets = null;
		if ( facetResults != null ) {
			facets = facetResults.get( facetingName );
		}
		if ( facets != null ) {
			return facets;
		}
		DocumentExtractor queryDocumentExtractor = query.queryDocumentExtractor();
		queryDocumentExtractor.close();
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
		FacetSelectionImpl selection = facetSelection.get( groupName );
		if ( selection == null ) {
			selection = new FacetSelectionImpl();
			facetSelection.put( groupName, selection );
		}
		return selection;
	}

	Map<String, FacetingRequestImpl> getFacetRequests() {
		return facetRequests;
	}

	void setFacetResults(Map<String, List<Facet>> facetResults) {
		this.facetResults = facetResults;
	}

	void queryHasChanged() {
		facetFilter = null;
		this.facetResults = null;
		query.clearCachedResults();
	}

	Filter getFacetFilter() {
		if ( facetFilter == null ) {
			BooleanQuery boolQuery = new BooleanQuery();
			for ( FacetSelectionImpl selection : facetSelection.values() ) {
				if ( !selection.getFacetList().isEmpty() ) {
					Query selectionGroupQuery = createSelectionGroupQuery( selection );
					boolQuery.add( selectionGroupQuery, BooleanClause.Occur.MUST );
				}
			}
			if ( boolQuery.getClauses().length > 0 ) {
				this.facetFilter = new QueryWrapperFilter( boolQuery );
			}
		}
		return facetFilter;
	}

	private Query createSelectionGroupQuery(FacetSelectionImpl selection) {
		BooleanQuery orQuery = new BooleanQuery();
		for ( Facet facet : selection.getFacetList() ) {
			orQuery.add( facet.getFacetQuery(), BooleanClause.Occur.SHOULD );
		}
		return orQuery;
	}

	class FacetSelectionImpl implements FacetSelection {
		private final List<Facet> facetList = newArrayList();

		public List<Facet> getFacetList() {
			return facetList;
		}

		@Override
		public void selectFacets(Facet... facets) {
			if ( facets == null ) {
				return;
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
	}
}


