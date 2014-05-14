/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.util.Counter;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.collector.impl.FacetCollector;
import org.hibernate.search.query.collector.impl.FieldCacheCollector;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.query.dsl.impl.FacetingRequestImpl;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.DistanceCollector;

/**
 * A helper class which gives access to the current query and its hits. This class will dynamically
 * reload the underlying {@code TopDocs} if required.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class QueryHits {

	private static final int DEFAULT_TOP_DOC_RETRIEVAL_SIZE = 100;

	private final LazyQueryState searcher;
	private final Filter filter;
	private final Sort sort;
	private final Map<String, FacetingRequestImpl> facetRequests;
	private final TimeoutManagerImpl timeoutManager;

	private int totalHits;
	private TopDocs topDocs;
	private Map<String, List<Facet>> facetMap;
	private List<FacetCollector> facetCollectors;
	private DistanceCollector distanceCollector = null;

	private final boolean enableFieldCacheOnClassName;

	private Coordinates spatialSearchCenter = null;
	private String spatialFieldName = null;

	/**
	 * If enabled, after hits collection it will contain the class name for each hit
	 */
	private FieldCacheCollector classTypeCollector;

	/**
	 * If enabled, a Collector will collect values from the primary keys
	 */
	private final FieldCacheCollectorFactory idFieldCollectorFactory;
	private FieldCacheCollector idFieldCollector;

	private final TimeoutExceptionFactory timeoutExceptionFactory;

	public QueryHits(LazyQueryState searcher,
					Filter filter,
					Sort sort,
					TimeoutManagerImpl timeoutManager,
					Map<String, FacetingRequestImpl> facetRequests,
					boolean enableFieldCacheOnTypes,
					FieldCacheCollectorFactory idFieldCollector,
					TimeoutExceptionFactory timeoutExceptionFactory,
					Coordinates spatialSearchCenter,
					String spatialFieldName)
			throws IOException {
		this(
				searcher, filter, sort, DEFAULT_TOP_DOC_RETRIEVAL_SIZE, timeoutManager, facetRequests,
				enableFieldCacheOnTypes, idFieldCollector, timeoutExceptionFactory, spatialSearchCenter, spatialFieldName
		);
	}

	public QueryHits(LazyQueryState searcher,
					Filter filter,
					Sort sort,
					Integer n,
					TimeoutManagerImpl timeoutManager,
					Map<String, FacetingRequestImpl> facetRequests,
					boolean enableFieldCacheOnTypes,
					FieldCacheCollectorFactory idFieldCollector,
					TimeoutExceptionFactory timeoutExceptionFactory,
					Coordinates spatialSearchCenter,
					String spatialFieldName)
			throws IOException {
		this.timeoutManager = timeoutManager;
		this.searcher = searcher;
		this.filter = filter;
		this.sort = sort;
		this.facetRequests = facetRequests;
		this.enableFieldCacheOnClassName = enableFieldCacheOnTypes;
		this.idFieldCollectorFactory = idFieldCollector;
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
	 * @param index
	 * @param fieldVisitor
	 * @throws IOException
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
		Collector collector = null;
		if ( maxDocs != 0 ) {
			topDocCollector = createTopDocCollector( maxDocs );
			hitCountCollector = null;
			collector = topDocCollector;
			collector = optionallyEnableFieldCacheOnTypes( collector, totalMaxDocs, maxDocs );
			collector = optionallyEnableFieldCacheOnIds( collector, totalMaxDocs, maxDocs );
			collector = optionallyEnableFacetingCollectors( collector );
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
			if ( facetCollectors != null && !facetCollectors.isEmpty() ) {
				facetMap = new HashMap<String, List<Facet>>();
				for ( FacetCollector facetCollector : facetCollectors ) {
					facetMap.put( facetCollector.getFacetName(), facetCollector.getFacetList() );
				}
			}
		}
		else {
			this.topDocs = null;
			this.totalHits = hitCountCollector.getTotalHits();
		}
		timeoutManager.isTimedOut();
	}

	private Collector optionallyEnableFacetingCollectors(Collector collector) {
		if ( facetRequests == null || facetRequests.isEmpty() ) {
			return collector;
		}
		facetCollectors = new ArrayList<FacetCollector>();
		Collector nextInChain = collector;
		for ( FacetingRequestImpl entry : facetRequests.values() ) {
			FacetCollector facetCollector = new FacetCollector( nextInChain, entry );
			nextInChain = facetCollector;
			facetCollectors.add( facetCollector );
		}

		return facetCollectors.get( facetCollectors.size() - 1 );
	}

	private Collector optionallyEnableDistanceCollector(Collector collector, int maxDocs) {
		if ( spatialFieldName == null || spatialFieldName.isEmpty() || spatialSearchCenter == null ) {
			return collector;
		}
		distanceCollector = new DistanceCollector( collector, spatialSearchCenter, maxDocs, spatialFieldName );

		return distanceCollector;
	}

	private boolean isImmediateTimeout() {
		boolean timeoutAt0 = false;
		if ( timeoutManager.getType() == TimeoutManager.Type.LIMIT ) {
			final Long timeoutLeft = timeoutManager.getTimeoutLeftInMilliseconds();
			if ( timeoutLeft != null ) {
				if ( timeoutLeft == 0l ) {
					if ( timeoutManager.getType() == TimeoutManager.Type.LIMIT && timeoutManager.isTimedOut() ) {
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
				maybeTimeLimitingCollector = new TimeLimitingCollector( collector, counter, timeoutLeft);
			}
		}
		return maybeTimeLimitingCollector;
	}

	private TopDocsCollector<?> createTopDocCollector(int maxDocs) throws IOException {
		TopDocsCollector<?> topCollector;
		if ( sort == null ) {
			topCollector = TopScoreDocCollector.create( maxDocs, !searcher.scoresDocsOutOfOrder() );
		}
		else {
			boolean fillFields = true;
			topCollector = TopFieldCollector.create(
					sort,
					maxDocs,
					fillFields,
					searcher.isFieldSortDoTrackScores(),
					searcher.isFieldSortDoMaxScore(),
					!searcher.scoresDocsOutOfOrder()
			);
		}
		return topCollector;
	}

	private Collector optionallyEnableFieldCacheOnIds(Collector collector, int totalMaxDocs, int maxDocs) {
		if ( idFieldCollectorFactory != null ) {
			idFieldCollector = idFieldCollectorFactory.createFieldCollector( collector, totalMaxDocs, maxDocs );
			return idFieldCollector;
		}
		return collector;
	}

	private Collector optionallyEnableFieldCacheOnTypes(Collector collector, int totalMaxDocs, int expectedMatchesCount) {
		if ( enableFieldCacheOnClassName ) {
			classTypeCollector = FieldCacheCollectorFactory
					.CLASS_TYPE_FIELD_CACHE_COLLECTOR_FACTORY
					.createFieldCollector( collector, totalMaxDocs, expectedMatchesCount );
			return classTypeCollector;
		}
		else {
			return collector;
		}
	}

	public FieldCacheCollector getClassTypeCollector() {
		return classTypeCollector;
	}

	public FieldCacheCollector getIdsCollector() {
		return idFieldCollector;
	}
}
