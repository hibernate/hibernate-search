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
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetRange;
import org.hibernate.search.query.facet.FacetRequest;
import org.hibernate.search.query.facet.FacetResult;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.RangeFacetRequest;
import org.hibernate.search.query.facet.SimpleFacetRequest;
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
	 * A container wrapping around the Lucene field cache data
	 */
	private final FieldCacheContainer fieldCache;

	/**
	 * A counter mapped to the field name for which it is counting
	 */
	private final Map<FieldName, FacetCounter<?>> facetCounts = newHashMap();

	/**
	 * Flag indicating whether the data structure has been initialised. Initialisation happens on the first call
	 * to {@link #setNextReader(org.apache.lucene.index.IndexReader, int)}.
	 */
	private boolean initialised = false;

	public FacetCollector(Collector delegate, Map<String, FacetRequest> facetRequests) {
		this.delegate = delegate;
		this.facetRequests = facetRequests;
		fieldCache = new FieldCacheContainer();
		for ( FacetRequest request : facetRequests.values() ) {
			FieldName key = createFieldName( request );
			FacetCounter counter = createFacetCounter( request );
			if ( !facetCounts.containsKey( key ) ) {
				facetCounts.put( key, counter );
			}
		}
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		if ( !initialised ) {
			initialiseCollector( reader );
		}
		initialiseFieldCaches( reader );
		delegate.setNextReader( reader, docBase );
	}

	@Override
	public void collect(int doc) throws IOException {
		for ( Map.Entry<FieldName, FacetCounter<?>> entry : facetCounts.entrySet() ) {
			FieldName fieldNameAndType = entry.getKey();
			if ( !fieldCache.containsCacheArray( fieldNameAndType ) ) {
				continue;
			}
			Object value = fieldCache.getCacheValue( fieldNameAndType, doc );
			// todo fix generics
			FacetCounter counter = entry.getValue();
			counter.countValue( value );
		}
		delegate.collect( doc );
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		delegate.setScorer( scorer );
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return delegate.acceptsDocsOutOfOrder();
	}

	public Map<String, FacetResult> getFacetResults() {
		Map<String, FacetResult> result = CollectionHelper.newHashMap();
		for ( Map.Entry<String, FacetRequest> entry : facetRequests.entrySet() ) {
			String facetRequestName = entry.getKey();
			FacetRequest request = entry.getValue();
			FacetCounter counter = facetCounts.get( createFieldName( request ) );
			List<Facet> facetList = createSortedFacetList( counter, request.getSort(), request.includeZeroCounts() );
			FacetResult facetResult = new FacetResult( request.getFieldName(), facetList );
			result.put( facetRequestName, facetResult );
		}
		return result;
	}

	private List<Facet> createSortedFacetList(FacetCounter<?> counter, FacetSortOrder sort, boolean includeZeroCounts) {
		List<Facet> facetList = newArrayList();
		for ( Map.Entry<String, Integer> countEntry : counter.getCounts().entrySet() ) {
			Facet facet = new Facet( countEntry.getKey(), countEntry.getValue() );
			if ( !includeZeroCounts && facet.getCount() == 0 ) {
				continue;
			}
			facetList.add( facet );
		}
		Collections.sort( facetList, new FacetComparator( sort ) );
		return facetList;
	}

	private void initialiseCollector(IndexReader reader) throws IOException {
		for ( Map.Entry<String, FacetRequest> entry : facetRequests.entrySet() ) {
			FacetRequest request = entry.getValue();
			// we only need to initialise the counts in case we have to include 0 counts as well
			if ( request.includeZeroCounts() && request instanceof SimpleFacetRequest ) {
				initFacetCounts( reader, request );
			}
		}
		initialised = true;
	}

	private void initialiseFieldCaches(IndexReader reader) throws IOException {
		Collection<String> existingFieldNames = reader.getFieldNames( IndexReader.FieldOption.ALL );
		for ( FacetRequest request : facetRequests.values() ) {
			String fieldName = request.getFieldName();
			if ( !existingFieldNames.contains( fieldName ) ) {
				continue;
			}
			FieldName nameAndType = createFieldName( request );
			fieldCache.storeCacheArray( reader, nameAndType, request.getFieldCacheType() );
		}
	}

	private FieldName createFieldName(FacetRequest request) {
		return new FieldName( request.getFieldName(), request.getFieldCacheType() );
	}

	private <N extends Number> FacetCounter createFacetCounter(FacetRequest request) {
		if ( request instanceof SimpleFacetRequest ) {
			return new SimpleFacetCounter();
		}
		else if ( request instanceof RangeFacetRequest ) {
			@SuppressWarnings("unchecked")
			RangeFacetRequest<N> rangeFacetRequest = (RangeFacetRequest<N>) request;
			return new RangeFacetCounter<N>( rangeFacetRequest );
		}
		else {
			throw new IllegalArgumentException( "Unsupported cache type" );
		}
	}

	private void initFacetCounts(IndexReader reader, FacetRequest request) throws IOException {
		String fieldName = request.getFieldName();
		FieldName key = createFieldName( request );
		// term are enumerated by field name and within field names by term value
		TermEnum terms = reader.terms( new Term( fieldName, "" ) );
		try {
			while ( fieldName.equals( terms.term().field() ) ) {
				String fieldValue = terms.term().text();
				FacetCounter counter = facetCounts.get( key );
				counter.initCount( fieldValue );
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

	static public abstract class FacetCounter<T> {
		private Map<String, Integer> counts = newHashMap();

		Map<String, Integer> getCounts() {
			return counts;
		}

		void initCount(String value) {
			if ( !counts.containsKey( value ) ) {
				counts.put( value, 0 );
			}
		}

		void incrementCount(String value) {
			if ( !counts.containsKey( value ) ) {
				counts.put( value, 1 );
			}
			else {
				counts.put( value, counts.get( value ) + 1 );
			}
		}

		abstract void countValue(T value);
	}

	static public class SimpleFacetCounter extends FacetCounter<String> {
		void countValue(String value) {
			incrementCount( value );
		}
	}

	static public class RangeFacetCounter<N extends Number> extends FacetCounter<N> {
		private final List<FacetRange<N>> ranges;

		RangeFacetCounter(RangeFacetRequest<N> request) {
			this.ranges = request.getFacetRangeList();
			for ( FacetRange<N> range : ranges ) {
				initCount( range.getRangeString() );
			}
		}

		void countValue(N value) {
			for ( FacetRange<N> range : ranges ) {
				if ( range.isInRange( value ) ) {
					incrementCount( range.getRangeString() );
				}
			}
		}
	}
}
