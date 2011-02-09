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

package org.hibernate.search.query;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.Scorer;

import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetRequest;
import org.hibernate.search.query.facet.FacetResult;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.util.CollectionHelper;

import static org.hibernate.search.util.CollectionHelper.newArrayList;
import static org.hibernate.search.util.CollectionHelper.newHashMap;

/**
 * A custom {@code Collector} used for handling facet requests.
 *
 * @author Hardy Ferentschik
 */
public class FacetCollector extends Collector {
	/**
	 * The original hit collector
	 */
	private final Collector delegate;

	/**
	 * Facet requests keyed against the user specified name of the request
	 */
	private final Map<String, FacetRequest> facetRequests;

	/**
	 * Field cache data keyed against the user specified name of the facet request
	 */
	private final Map<String, String[]> fieldCache = newHashMap();

	/**
	 * Count per field value keyed against the user specified name of the facet request
	 */
	private final Map<String, Map<String, Integer>> facetCounts = newHashMap();

	public FacetCollector(Collector delegate, Map<String, FacetRequest> facetRequests) {
		this.delegate = delegate;
		this.facetRequests = facetRequests;
		for ( Map.Entry<String, FacetRequest> entry : facetRequests.entrySet() ) {
			facetCounts.put( entry.getKey(), new HashMap<String, Integer>() );
		}
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		delegate.setScorer( scorer );
	}

	@Override
	public void collect(int doc) throws IOException {
		for ( Map.Entry<String, String[]> entry : fieldCache.entrySet() ) {
			Map<String, Integer> countMap = facetCounts.get( entry.getKey() );
			String value = entry.getValue()[doc];
			if ( !countMap.containsKey( value ) ) {
				countMap.put( value, 1 );
			}
			else {
				countMap.put( value, countMap.get( value ) + 1 );
			}
		}
		delegate.collect( doc );
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		initialiseFieldCaches( reader );
		delegate.setNextReader( reader, docBase );
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return delegate.acceptsDocsOutOfOrder();
	}

	public Map<String, FacetResult> getFacetResults() {
		Map<String, FacetResult> result = CollectionHelper.newHashMap();
		for ( Map.Entry<String, Map<String, Integer>> entry : facetCounts.entrySet() ) {
			String facetRequestName = entry.getKey();
			FacetRequest request = facetRequests.get( facetRequestName );
			Map<String, Integer> countMap = entry.getValue();
			List<Facet> facetList = createSortedFacetList( countMap, request.getSort() );
			FacetResult facetResult = new FacetResult( request.getFieldName(), facetList );
			result.put( facetRequestName, facetResult );
		}
		return result;
	}

	private List<Facet> createSortedFacetList(Map<String, Integer> countMap, FacetSortOrder sort) {
		List<Facet> facetList = newArrayList();
		for ( Map.Entry<String, Integer> countEntry : countMap.entrySet() ) {
			Facet facet = new Facet( countEntry.getKey(), countEntry.getValue() );
			facetList.add( facet );
		}
		Collections.sort( facetList, new FacetComparator( sort ) );
		return facetList;
	}

	private void initialiseFieldCaches(IndexReader reader) throws IOException {
		Collection<String> existingFieldNames = reader.getFieldNames( IndexReader.FieldOption.ALL );
		for ( Map.Entry<String, FacetRequest> entry : facetRequests.entrySet() ) {
			FacetRequest request = entry.getValue();
			String facetName = entry.getKey();
			String fieldName = request.getFieldName();
			if ( !existingFieldNames.contains( fieldName ) ) {
				continue;
			}
			String[] cache = FieldCache.DEFAULT.getStrings( reader, fieldName );
			fieldCache.put( facetName, cache );
			// we only need to initialise the counts in case we have to include 0 counts as well
			if ( request.includeZeroCounts() ) {
				initFacetCounts( reader, facetName, request );
			}
		}
	}

	private void initFacetCounts(IndexReader reader, String facetName, FacetRequest request) throws IOException {
		String fieldName = request.getFieldName();
		// term are enumerated by field name and within field names by term value
		TermEnum terms = reader.terms( new Term( fieldName, "" ) );
		try {
			while ( fieldName.equals( terms.term().field() ) ) {
				String fieldValue = terms.term().text();
				Map<String, Integer> countsPerValue = facetCounts.get( facetName );
				if ( !countsPerValue.containsKey( fieldValue ) ) {
					countsPerValue.put( fieldValue, 0 );
				}
				if ( !terms.next() ) {
					break;
				}
			}
		}
		finally {
			terms.close();
		}
	}

	static public class FacetComparator implements Comparator<Facet> {
		private final FacetSortOrder sortOder;

		public FacetComparator(FacetSortOrder sortOrder) {
			this.sortOder = sortOrder;
		}

		public int compare(Facet facet1, Facet facet2) {
			if ( FacetSortOrder.COUNT_ASC.equals( sortOder ) ) {
				return facet1.getCount() - facet2.getCount();
			}
			else if ( FacetSortOrder.COUNT_DESC.equals( sortOder ) ) {
				return facet2.getCount() - facet1.getCount();
			}
			else {
				return facet1.getValue().compareTo( facet2.getValue() );
			}
		}
	}
}
