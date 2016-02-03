/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.range.DoubleRange;
import org.apache.lucene.facet.range.DoubleRangeFacetCounts;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Counter;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.DistanceCollector;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A helper class which gives access to the current query and its hits. This class will dynamically
 * reload the underlying {@code TopDocs} if required.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class QueryHits {
	private static final Log log = LoggerFactory.make();

	private static final int DEFAULT_TOP_DOC_RETRIEVAL_SIZE = 100;
	private static final int DEFAULT_FACET_RETRIEVAL_SIZE = 100;
	private static final EnumMap<FacetSortOrder, FacetComparator> facetComparators = new EnumMap<>( FacetSortOrder.class );

	static {
		facetComparators.put( FacetSortOrder.COUNT_ASC, new FacetComparator( FacetSortOrder.COUNT_ASC ) );
		facetComparators.put( FacetSortOrder.COUNT_DESC, new FacetComparator( FacetSortOrder.COUNT_DESC ) );
		facetComparators.put( FacetSortOrder.FIELD_VALUE, new FacetComparator( FacetSortOrder.FIELD_VALUE ) );
	}

	private final LazyQueryState searcher;
	private final Filter filter;
	private final Sort sort;
	private final Map<String, FacetingRequestImpl> facetRequests;
	private final TimeoutManagerImpl timeoutManager;

	private int totalHits;
	private TopDocs topDocs;
	private Map<String, List<Facet>> facetMap;
	private FacetsCollector facetsCollector;
	private DistanceCollector distanceCollector = null;

	private Coordinates spatialSearchCenter = null;
	private String spatialFieldName = null;

	private final TimeoutExceptionFactory timeoutExceptionFactory;

	public QueryHits(LazyQueryState searcher,
			Filter filter,
			Sort sort,
			TimeoutManagerImpl timeoutManager,
			Map<String, FacetingRequestImpl> facetRequests,
			TimeoutExceptionFactory timeoutExceptionFactory,
			Coordinates spatialSearchCenter,
			String spatialFieldName)
			throws IOException {
		this(
				searcher,
				filter,
				sort,
				DEFAULT_TOP_DOC_RETRIEVAL_SIZE,
				timeoutManager,
				facetRequests,
				timeoutExceptionFactory,
				spatialSearchCenter,
				spatialFieldName
		);
	}

	public QueryHits(LazyQueryState searcher,
			Filter filter,
			Sort sort,
			Integer n,
			TimeoutManagerImpl timeoutManager,
			Map<String, FacetingRequestImpl> facetRequests,
			TimeoutExceptionFactory timeoutExceptionFactory,
			Coordinates spatialSearchCenter,
			String spatialFieldName)
			throws IOException {
		this.timeoutManager = timeoutManager;
		this.searcher = searcher;
		this.filter = filter;
		this.sort = sort;
		this.facetRequests = facetRequests;
		this.timeoutExceptionFactory = timeoutExceptionFactory;
		this.spatialSearchCenter = spatialSearchCenter;
		this.spatialFieldName = spatialFieldName;
		updateTopDocs( n );
	}

	public Document doc(int index) throws IOException {
		return searcher.doc( docId( index ) );
	}

	/**
	 * This document loading strategy doesn't return anything as it's the responsibility
	 * of the passed StoredFieldVisitor instance to collect the data it needs.
	 *
	 * @param index {@link ScoreDoc} index
	 * @param fieldVisitor accessor to the stored field value in the index
	 *
	 * @throws IOException if an error occurs accessing the index
	 */
	public void visitDocument(int index, StoredFieldVisitor fieldVisitor) throws IOException {
		searcher.doc( docId( index ), fieldVisitor );
	}

	public ScoreDoc scoreDoc(int index) throws IOException {
		if ( index >= totalHits ) {
			throw new SearchException( "Not a valid ScoreDoc index: " + index );
		}

		// TODO - Is there a better way to get more TopDocs? Get more or less?
		if ( index >= topDocs.scoreDocs.length ) {
			updateTopDocs( 2 * index );
		}
		//if the refresh timed out, raise an exception
		if ( timeoutManager.isTimedOut() && index >= topDocs.scoreDocs.length ) {
			throw timeoutExceptionFactory.createTimeoutException(
					"Timeout period exceeded. Cannot load document: " + index,
					searcher.describeQuery()
			);
		}
		return topDocs.scoreDocs[index];
	}

	public int docId(int index) throws IOException {
		return scoreDoc( index ).doc;
	}

	public float score(int index) throws IOException {
		return scoreDoc( index ).score;
	}

	public Double spatialDistance(int index) throws IOException {
		if ( spatialSearchCenter == null ) {
			return null;
		}
		return Double.valueOf( distanceCollector.getDistance( docId( index ) ) );
	}

	public Explanation explain(int index) throws IOException {
		final Explanation explanation = searcher.explain( docId( index ) );
		timeoutManager.isTimedOut();
		return explanation;
	}

	public int getTotalHits() {
		return totalHits;
	}

	public TopDocs getTopDocs() {
		return topDocs;
	}

	public Map<String, List<Facet>> getFacets() {
		if ( facetRequests == null || facetRequests.size() == 0 ) {
			return Collections.emptyMap();
		}
		return facetMap;
	}

	/**
	 * @param n the number of {@code TopDoc}s to retrieve. The actual retrieved number of {@code TopDoc}s is n or the
	 * total number of documents if {@code n > maxDoc}
	 *
	 * @throws IOException in case a search exception occurs
	 */
	private void updateTopDocs(int n) throws IOException {
		final int totalMaxDocs = searcher.maxDoc();
		final int maxDocs = Math.min( n, totalMaxDocs );

		final TopDocsCollector<?> topDocCollector;
		final TotalHitCountCollector hitCountCollector;
		Collector collector;
		if ( maxDocs != 0 ) {
			topDocCollector = createTopDocCollector( maxDocs );
			hitCountCollector = null;
			collector = topDocCollector;
			collector = optionallyEnableFacetingCollector( collector );
			collector = optionallyEnableDistanceCollector( collector, maxDocs );
		}
		else {
			topDocCollector = null;
			hitCountCollector = new TotalHitCountCollector();
			collector = hitCountCollector;
		}
		collector = decorateWithTimeOutCollector( collector );

		boolean timeoutNow = isImmediateTimeout();
		if ( !timeoutNow ) {
			try {
				searcher.search( filter, collector );
			}
			catch (TimeLimitingCollector.TimeExceededException e) {
				//we have reached the time limit and stopped before the end
				//TimeoutManager.isTimedOut should be above that limit but set if for safety
				timeoutManager.forceTimedOut();
			}
		}

		// update top docs and totalHits
		if ( maxDocs != 0 ) {
			this.topDocs = topDocCollector.topDocs();
			this.totalHits = topDocs.totalHits;
			// if we were collecting facet data we have to update our instance state
			if ( facetsCollector != null ) {
				updateFacets();
			}
		}
		else {
			this.topDocs = null;
			this.totalHits = hitCountCollector.getTotalHits();
		}
		timeoutManager.isTimedOut();
	}

	private void updateFacets() throws IOException {
		facetMap = new HashMap<>();
		for ( FacetingRequestImpl facetRequest : facetRequests.values() ) {
			ArrayList<Facet> facets;
			if ( facetRequest instanceof DiscreteFacetRequest ) {
				facets = updateStringFacets( (DiscreteFacetRequest) facetRequest );
			}
			else {
				facets = updateRangeFacets( (RangeFacetRequest<?>) facetRequest );
			}

			// sort if necessary
			if ( !facetRequest.getSort().equals( FacetSortOrder.RANGE_DEFINITION_ORDER ) ) {
				Collections.sort( facets, facetComparators.get( facetRequest.getSort() ) );
			}

			// trim to the expected size
			int maxNumberOfExpectedFacets = facetRequest.getMaxNumberOfFacets();
			if ( maxNumberOfExpectedFacets > 0 && facets.size() > maxNumberOfExpectedFacets ) {
				facets = new ArrayList<>( facets.subList( 0, facetRequest.getMaxNumberOfFacets() ) );
			}

			facetMap.put( facetRequest.getFacetingName(), facets );
		}
	}

	private ArrayList<Facet> updateRangeFacets(RangeFacetRequest<?> facetRequest) throws IOException {
		ArrayList<Facet> facets;
		if ( ReflectionHelper.isIntegerType( facetRequest.getFacetValueType() )
				|| Date.class.isAssignableFrom( facetRequest.getFacetValueType() ) ) {
			FacetResult facetResult = getFacetResultForLongRange( facetRequest );
			facets = new ArrayList<>( facetResult.labelValues.length );
			for ( LabelAndValue labelAndValue : facetResult.labelValues ) {
				if ( !facetRequest.hasZeroCountsIncluded() && (int) labelAndValue.value == 0 ) {
					continue;
				}

				Facet facet = facetRequest.createFacet( labelAndValue.label, (int) labelAndValue.value );
				facets.add( facet );
			}
		}
		else if ( ReflectionHelper.isFloatingPointType( facetRequest.getFacetValueType() ) ) {
			FacetResult facetResult = getFacetResultForFloatingPointRange( facetRequest );
			facets = new ArrayList<>( facetResult.labelValues.length );
			for ( LabelAndValue labelAndValue : facetResult.labelValues ) {
				if ( !facetRequest.hasZeroCountsIncluded() && (int) labelAndValue.value == 0 ) {
					continue;
				}

				Facet facet = facetRequest.createFacet( labelAndValue.label, (int) labelAndValue.value );
				facets.add( facet );
			}
		}
		else {
			throw log.unsupportedFacetRangeParameter( facetRequest.getFacetValueType().getName() );
		}

		return facets;
	}

	private FacetResult getFacetResultForFloatingPointRange(RangeFacetRequest<?> facetRequest) throws IOException {
		List<? extends FacetRange<?>> facetRanges = facetRequest.getFacetRangeList();
		DoubleRange[] ranges = new DoubleRange[facetRanges.size()];

		int i = 0;
		for ( FacetRange<?> facetRange : facetRanges ) {
			ranges[i] = new DoubleRange(
					facetRange.getRangeString(),
					facetRange.getMin() == null ? Double.MIN_VALUE : ( (Number) facetRange.getMin() ).doubleValue(),
					facetRange.isMinIncluded(),
					facetRange.getMax() == null ? Double.MAX_VALUE : ( (Number) facetRange.getMax() ).doubleValue(),
					facetRange.isMaxIncluded()
			);
			i++;
		}

		DoubleRangeFacetCounts facetCount = new DoubleRangeFacetCounts(
				facetRequest.getFieldName(),
				facetsCollector,
				ranges
		);
		return facetCount.getTopChildren(
				facetRequest.getMaxNumberOfFacets(),
				facetRequest.getFieldName()
		);
	}

	private FacetResult getFacetResultForLongRange(RangeFacetRequest<?> facetRequest)
			throws IOException {
		List<? extends FacetRange<?>> facetRanges = facetRequest.getFacetRangeList();
		LongRange[] ranges = new LongRange[facetRanges.size()];

		int i = 0;
		for ( FacetRange<?> facetRange : facetRanges ) {
			long min;
			long max;

			if ( ReflectionHelper.isIntegerType( facetRequest.getFacetValueType() ) ) {
				min = facetRange.getMin() == null ? Long.MIN_VALUE : ( (Number) facetRange.getMin() ).longValue();
				max = facetRange.getMax() == null ? Long.MAX_VALUE : ( (Number) facetRange.getMax() ).longValue();
			}
			else {
				min = facetRange.getMin() == null ? Long.MIN_VALUE : ( (Date) facetRange.getMin() ).getTime();
				max = facetRange.getMax() == null ? Long.MAX_VALUE : ( (Date) facetRange.getMax() ).getTime();
			}

			ranges[i] = new LongRange(
					facetRange.getRangeString(),
					min,
					facetRange.isMinIncluded(),
					max,
					facetRange.isMaxIncluded()
			);
			i++;
		}

		LongRangeFacetCounts facetCount = new LongRangeFacetCounts(
				facetRequest.getFieldName(),
				facetsCollector,
				ranges
		);
		return facetCount.getTopChildren(
				facetRequest.getMaxNumberOfFacets(),
				facetRequest.getFieldName()
		);
	}

	private ArrayList<Facet> updateStringFacets(DiscreteFacetRequest facetRequest) throws IOException {
		SortedSetDocValuesReaderState docValuesReaderState;
		try {
			docValuesReaderState = new DefaultSortedSetDocValuesReaderState( searcher.getIndexReader() );
		}
		catch (IllegalArgumentException e) {
			// happens in case there are no facets at all configured for the matching documents
			throw log.unknownFieldNameForFaceting( facetRequest.getFacetingName(), facetRequest.getFieldName() );
		}
		SortedSetDocValuesFacetCounts facetCounts = new SortedSetDocValuesFacetCounts(
				docValuesReaderState, facetsCollector
		);

		Set<String> termValues = Collections.emptySet();
		if ( facetRequest.hasZeroCountsIncluded() ) {
			termValues = findAllTermsForField( facetRequest.getFieldName(), searcher.getIndexReader() );
		}

		int maxFacetCount = facetRequest.getMaxNumberOfFacets() < 0 ? DEFAULT_FACET_RETRIEVAL_SIZE : facetRequest.getMaxNumberOfFacets();
		final FacetResult facetResult;
		try {
			// This might return null!
			facetResult = facetCounts.getTopChildren( maxFacetCount, facetRequest.getFieldName() );
		}
		catch (IllegalArgumentException e) {
			// happens in case there are facets in general, but not for this specific field
			throw log.unknownFieldNameForFaceting( facetRequest.getFacetingName(), facetRequest.getFieldName() );
		}
		ArrayList<Facet> facets = new ArrayList<>();
		if ( facetResult != null ) {
			for ( LabelAndValue labelAndValue : facetResult.labelValues ) {
				Facet facet = facetRequest.createFacet( labelAndValue.label, (int) labelAndValue.value );
				facets.add( facet );
				termValues.remove( labelAndValue.label );
			}
		}
		for ( String termValue : termValues ) {
			Facet facet = facetRequest.createFacet( termValue, 0 );
			facets.add( 0, facet );
		}
		return facets;
	}

	/**
	 * Returns a set of all possible indexed values for a given field name.
	 *
	 * Per default Lucene's native discrete faceting won't return values where
	 * the count would be 0 for a given query. To still include these "zero count values"
	 * we need to go the the index and find all potential values for a field and compare them
	 * against the values which had a count. This might potentially be expensive, so returning
	 * zero count values is per default disabled.
	 *
	 * @param fieldName the document field name
	 * @param reader the index reader
	 * @return a set of all possible indexed values for a given field name
	 * @throws IOException
	 */
	private Set<String> findAllTermsForField(String fieldName, IndexReader reader) throws IOException {
		Set<String> termValues = new HashSet<>();
		for ( LeafReaderContext leaf : reader.leaves() ) {
			final LeafReader atomicReader = leaf.reader();
			Terms terms = atomicReader.terms( fieldName );
			if ( terms == null ) {
				continue;
			}
			final TermsEnum iterator = terms.iterator();
			BytesRef byteRef;
			while ( ( byteRef = iterator.next() ) != null ) {
				termValues.add( byteRef.utf8ToString() );
			}
		}
		return termValues;
	}

	private Collector optionallyEnableFacetingCollector(Collector collector) {
		if ( facetRequests == null || facetRequests.isEmpty() ) {
			return collector;
		}
		facetsCollector = new FacetsCollector();
		return MultiCollector.wrap( facetsCollector, collector );
	}

	private Collector optionallyEnableDistanceCollector(Collector collector, int maxDocs) {
		if ( spatialFieldName == null || spatialFieldName.isEmpty() || spatialSearchCenter == null ) {
			return collector;
		}
		distanceCollector = new DistanceCollector( spatialSearchCenter, maxDocs, spatialFieldName );

		return MultiCollector.wrap( distanceCollector, collector );
	}

	private boolean isImmediateTimeout() {
		boolean timeoutAt0 = false;
		if ( timeoutManager.getType() == TimeoutManager.Type.LIMIT ) {
			final Long timeoutLeft = timeoutManager.getTimeoutLeftInMilliseconds();
			if ( timeoutLeft != null ) {
				if ( timeoutLeft == 0l ) {
					if ( timeoutManager.isTimedOut() ) {
						timeoutManager.forceTimedOut();
						timeoutAt0 = true;
					}
				}
			}
			else {
				if ( timeoutManager.isTimedOut() ) {
					timeoutManager.forceTimedOut();
				}
			}
		}
		return timeoutAt0;
	}

	private Collector decorateWithTimeOutCollector(Collector collector) {
		Collector maybeTimeLimitingCollector = collector;
		if ( timeoutManager.getType() == TimeoutManager.Type.LIMIT ) {
			final Long timeoutLeft = timeoutManager.getTimeoutLeftInMilliseconds();
			if ( timeoutLeft != null ) {
				Counter counter = timeoutManager.getLuceneTimeoutCounter();
				maybeTimeLimitingCollector = new TimeLimitingCollector( collector, counter, timeoutLeft );
			}
		}
		return maybeTimeLimitingCollector;
	}

	private TopDocsCollector<?> createTopDocCollector(int maxDocs) throws IOException {
		TopDocsCollector<?> topCollector;
		if ( sort == null ) {
			topCollector = TopScoreDocCollector.create( maxDocs );
		}
		else {
			boolean fillFields = true;
			topCollector = TopFieldCollector.create(
					sort,
					maxDocs,
					fillFields,
					searcher.isFieldSortDoTrackScores(),
					searcher.isFieldSortDoMaxScore()
			);
		}
		return topCollector;
	}

	public static class FacetComparator implements Comparator<Facet>, Serializable {
		private final FacetSortOrder sortOder;

		public FacetComparator(FacetSortOrder sortOrder) {
			this.sortOder = sortOrder;
		}

		@Override
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
