/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.dsl.impl.RangeFacetImpl;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.fieldcache.impl.FieldCacheLoadingType;
import org.hibernate.search.query.fieldcache.impl.FieldLoadingStrategy;

import static org.hibernate.search.util.impl.CollectionHelper.newArrayList;

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
	 * to {@link #setNextReader(AtomicReaderContext)}.
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
	public void setNextReader(AtomicReaderContext context) throws IOException {
		if ( !initialised ) {
			initialiseCollector( context );
		}
		initialiseFieldCaches( context );
		nextInChainCollector.setNextReader( context );
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
		if ( FacetSortOrder.RANGE_DEFINITION_ORDER.equals( request.getSort() ) ) {
			facetList = createRangeFacetList( counter.getCounts().entrySet(), request, counter.getCounts().size() );
			Collections.sort( facetList, new RangeDefinitionOrderFacetComparator() );
			if ( facetRequest.getMaxNumberOfFacets() > 0 ) {
				facetList = facetList.subList( 0, Math.min( facetRequest.getMaxNumberOfFacets(), facetList.size() ) );
			}
		}
		else {
			List<Map.Entry<String, IntegerWrapper>> countEntryList = newArrayList( counter.getCounts().size() );
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
		List<Facet> facetList = newArrayList( countEntryList.size() );
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

	private void initialiseCollector(AtomicReaderContext context) throws IOException {
		// we only need to initialise the counts in case we have to include 0 counts as well
		if ( facetRequest.hasZeroCountsIncluded() && facetRequest instanceof DiscreteFacetRequest ) {
			initFacetCounts( context );
		}
		initialised = true;
	}

	private void initialiseFieldCaches(AtomicReaderContext context) throws IOException {
		fieldLoader.loadNewCacheValues( context );
	}

	private <N extends Number> FacetCounter createFacetCounter(FacetingRequestImpl request) {
		if ( request instanceof DiscreteFacetRequest ) {
			return new FacetCounter.SimpleFacetCounter();
		}
		else if ( request instanceof RangeFacetRequest ) {
			@SuppressWarnings("unchecked")
			RangeFacetRequest<N> rangeFacetRequest = (RangeFacetRequest<N>) request;
			return new FacetCounter.RangeFacetCounter<N>( rangeFacetRequest );
		}
		else {
			throw new IllegalArgumentException( "Unsupported cache type" );
		}
	}

	private void initFacetCounts(AtomicReaderContext context) throws IOException {
		String fieldName = facetRequest.getFieldName();
		// term are enumerated by field name and within field names by term value
		final AtomicReader atomicReader = context.reader();
		final Terms terms = atomicReader.terms( fieldName );
		if ( terms == null ) {
			//if the Reader has no fields at all,
			//undefined field or no matches
			return;
		}
		final TermsEnum iterator = terms.iterator( null ); //we have no TermsEnum to reuse
		BytesRef byteRef;
		while ( ( byteRef = iterator.next() ) != null ) {
			final String fieldValue = byteRef.utf8ToString();
			facetCounts.initCount( fieldValue );
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
}
