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

package org.hibernate.search.query.collector.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.dsl.impl.RangeFacetImpl;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.fieldcache.impl.FieldCacheLoadingType;
import org.hibernate.search.query.fieldcache.impl.FieldLoadingStrategy;

import static org.hibernate.search.util.impl.CollectionHelper.newArrayList;
import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

/**
 * A custom {@code Collector} used for handling facet requests.
 *
 * @author Hardy Ferentschik
 */
public class FacetCollector extends Collector {
	/**
	 * The next collector in the delegation chain
	 */
	private final Collector nextInChainCollector;

	/**
	 * Facet request this collector handles
	 */
	private final FacetingRequestImpl facetRequest;

	/**
	 * Used to load field values from the Lucene field cache
	 */
	private final FieldLoadingStrategy fieldLoader;

	/**
	 * A counter mapped to the field name for which it is counting
	 */
	private final FacetCounter facetCounts;

	/**
	 * Flag indicating whether the data structure has been initialised. Initialisation happens on the first call
	 * to {@link #setNextReader(org.apache.lucene.index.IndexReader, int)}.
	 */
	private boolean initialised = false;

	public FacetCollector(Collector nextInChainCollector, FacetingRequestImpl facetRequest) {
		this.nextInChainCollector = nextInChainCollector;
		this.facetRequest = facetRequest;
		this.facetCounts = createFacetCounter( facetRequest );
		fieldLoader = FieldCacheLoadingType.getLoadingStrategy(
				this.facetRequest.getFieldName(), this.facetRequest.getFieldCacheType()
		);
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		if ( !initialised ) {
			initialiseCollector( reader );
		}
		initialiseFieldCaches( reader );
		nextInChainCollector.setNextReader( reader, docBase );
	}

	@Override
	public void collect(int doc) throws IOException {
		Object value = fieldLoader.collect( doc );
		if ( value != null ) {
			facetCounts.countValue( value );
		}
		nextInChainCollector.collect( doc );
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		nextInChainCollector.setScorer( scorer );
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return nextInChainCollector.acceptsDocsOutOfOrder();
	}

	public String getFacetName() {
		return facetRequest.getFacetingName();
	}

	public List<Facet> getFacetList() {
		return createSortedFacetList( facetCounts, facetRequest );
	}

	private List<Facet> createSortedFacetList(FacetCounter counter, FacetingRequestImpl request) {
		List<Facet> facetList;
		// handle RANGE_DEFINITION_ODER differently from count based orders. we try to avoid the creation of
		// Facet instances which we can only do for count based ordering
		if ( FacetSortOrder.RANGE_DEFINITION_ODER.equals( request.getSort() )
				|| FacetSortOrder.RANGE_DEFINITION_ORDER.equals( request.getSort() ) ) {
			facetList = createRangeFacetList( counter.getCounts().entrySet(), request, counter.getCounts().size() );
			Collections.sort( facetList, new RangeDefinitionOrderFacetComparator( ) );
			if ( facetRequest.getMaxNumberOfFacets() > 0 ) {
				facetList = facetList.subList( 0, Math.min( facetRequest.getMaxNumberOfFacets(), facetList.size() ) );
			}
		}
		else {
			List<Map.Entry<String, IntegerWrapper>> countEntryList = newArrayList();
			for ( Entry<String, IntegerWrapper> stringIntegerEntry : counter.getCounts().entrySet() ) {
				countEntryList.add( stringIntegerEntry );
			}
			int facetCount = facetRequest.getMaxNumberOfFacets() > 0 ?
					facetRequest.getMaxNumberOfFacets() : countEntryList.size();
			Collections.sort( countEntryList, new FacetEntryComparator( request.getSort() ) );
			facetList = createRangeFacetList( countEntryList, request, facetCount );
		}
		return facetList;
	}

	private List<Facet> createRangeFacetList(Collection<Entry<String, IntegerWrapper>> countEntryList, FacetingRequestImpl request, int count) {
		List<Facet> facetList = newArrayList();
		int includedFacetCount = 0;
		for ( Map.Entry<String, IntegerWrapper> countEntry : countEntryList ) {
			Facet facet = request.createFacet( countEntry.getKey(), countEntry.getValue().getCount() );
			if ( !request.hasZeroCountsIncluded() && facet.getCount() == 0 ) {
				continue;
			}
			facetList.add( facet );
			includedFacetCount++;
			if ( includedFacetCount == count ) {
				break;
			}
		}
		return facetList;
	}

	private void initialiseCollector(IndexReader reader) throws IOException {
		// we only need to initialise the counts in case we have to include 0 counts as well
		if ( facetRequest.hasZeroCountsIncluded() && facetRequest instanceof DiscreteFacetRequest ) {
			initFacetCounts( reader );
		}
		initialised = true;
	}

	private void initialiseFieldCaches(IndexReader reader) throws IOException {
		fieldLoader.loadNewCacheValues( reader );
	}

	private <N extends Number> FacetCounter createFacetCounter(FacetingRequestImpl request) {
		if ( request instanceof DiscreteFacetRequest ) {
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

	private void initFacetCounts(IndexReader reader) throws IOException {
		String fieldName = facetRequest.getFieldName();
		// term are enumerated by field name and within field names by term value
		TermEnum terms = reader.terms( new Term( fieldName, "" ) );
		try {
			while ( fieldName.equals( terms.term().field() ) ) {
				String fieldValue = terms.term().text();
				facetCounts.initCount( fieldValue );
				if ( !terms.next() ) {
					break;
				}
			}
		}
		finally {
			terms.close();
		}
	}

	public static class FacetEntryComparator implements Comparator<Entry<String, IntegerWrapper>>, Serializable {
		private final FacetSortOrder sortOder;

		public FacetEntryComparator(FacetSortOrder sortOrder) {
			this.sortOder = sortOrder;
		}

		@Override
		public int compare(Entry<String, IntegerWrapper> entry1, Entry<String, IntegerWrapper> entry2) {
			if ( FacetSortOrder.COUNT_ASC.equals( sortOder ) ) {
				return entry1.getValue().getCount() - entry2.getValue().getCount();
			}
			else if ( FacetSortOrder.COUNT_DESC.equals( sortOder ) ) {
				return entry2.getValue().getCount() - entry1.getValue().getCount();
			}
			else {
				return entry1.getKey().compareTo( entry2.getKey() );
			}
		}
	}

	public static class RangeDefinitionOrderFacetComparator implements Comparator<Facet>, Serializable {

		@Override
		public int compare(Facet facet1, Facet facet2) {
			return ( (RangeFacetImpl) facet1 ).getRangeIndex() - ( (RangeFacetImpl) facet2 ).getRangeIndex();
		}
	}

	public abstract static class FacetCounter {
		private Map<String, IntegerWrapper> counts = newHashMap();

		Map<String, IntegerWrapper> getCounts() {
			return counts;
		}

		void initCount(String value) {
			if ( !counts.containsKey( value ) ) {
				counts.put( value, new IntegerWrapper() );
			}
		}

		void incrementCount(String value) {
			IntegerWrapper integerWrapper = counts.get( value );
			if ( integerWrapper == null ) {
				integerWrapper = new IntegerWrapper();
				counts.put( value, integerWrapper );
			}
			integerWrapper.incrementCount();
		}

		abstract void countValue(Object value);
	}

	static class SimpleFacetCounter extends FacetCounter {
		@Override
		void countValue(Object value) {
			incrementCount( (String) value );
		}
	}

	static class RangeFacetCounter<T> extends FacetCounter {
		private final List<FacetRange<T>> ranges;

		RangeFacetCounter(RangeFacetRequest<T> request) {
			this.ranges = request.getFacetRangeList();
			for ( FacetRange<T> range : ranges ) {
				initCount( range.getRangeString() );
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		void countValue(Object value) {
			for ( FacetRange<T> range : ranges ) {
				if ( range.isInRange( (T) value ) ) {
					incrementCount( range.getRangeString() );
				}
			}
		}
	}

	/**
	 * Replacement of Integer which is mutable,
	 * so that we can avoid creating many objects
	 * while counting hits for each facet.
	 */
	private static final class IntegerWrapper {
		int count = 0;

		public int getCount() {
			return count;
		}

		public void incrementCount() {
			this.count++;
		}
	}

}
