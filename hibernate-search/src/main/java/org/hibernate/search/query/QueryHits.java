/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.sql.SQLException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
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
import org.apache.lucene.search.Weight;

import org.hibernate.QueryTimeoutException;
import org.hibernate.search.SearchException;

/**
 * A helper class which gives access to the current query and its hits. This class will dynamically
 * reload the underlying <code>TopDocs</code> if required.
 *
 * @author Hardy Ferentschik
 */
public class QueryHits {

	private static final int DEFAULT_TOP_DOC_RETRIEVAL_SIZE = 100;
	public final org.apache.lucene.search.Query preparedQuery;
	//we only accept indexSearcher atm which means we can copy its the Collector creation strategy
	public final IndexSearcherWithPayload searcher;
	public final Filter filter;
	public final Sort sort;
	public int totalHits;
	public TopDocs topDocs;
	private TimeoutManager timeoutManager;

	public QueryHits(IndexSearcherWithPayload searcher,
					 org.apache.lucene.search.Query preparedQuery,
					 Filter filter,
					 Sort sort,
					 TimeoutManager timeoutManager)
			throws IOException {
		this( searcher, preparedQuery, filter, sort, DEFAULT_TOP_DOC_RETRIEVAL_SIZE, timeoutManager );
	}

	public QueryHits(IndexSearcherWithPayload searcher,
					 org.apache.lucene.search.Query preparedQuery,
					 Filter filter,
					 Sort sort,
					 Integer n,
					 TimeoutManager timeoutManager )
			throws IOException {
		this.timeoutManager = timeoutManager;
		this.preparedQuery = preparedQuery;
		this.searcher = searcher;
		this.filter = filter;
		this.sort = sort;
		updateTopDocs( n );
		this.totalHits = topDocs.totalHits;
	}

	public Document doc(int index) throws IOException {
		return searcher.getSearcher().doc( docId( index ) );
	}

	public Document doc(int index, FieldSelector selector) throws IOException {
		return searcher.getSearcher().doc( docId( index ), selector );
	}

	public ScoreDoc scoreDoc(int index) throws IOException {
		if ( index >= totalHits ) {
		  throw new SearchException("Not a valid ScoreDoc index: " + index);
		}

		// TODO - Is there a better way to get more TopDocs? Get more or less?
		if ( index >= topDocs.scoreDocs.length ) {
			updateTopDocs( 2 * index );
		}
		//if the refresh timed out, raise an exception
		if ( timeoutManager.isTimedOut() && index >= topDocs.scoreDocs.length ) {
			throw new QueryTimeoutException("Timeout period exceeded. Cannot load document: " + index, (SQLException) null, preparedQuery.toString() );
		}
		return topDocs.scoreDocs[index];
	}

	public int docId(int index) throws IOException {
		return scoreDoc( index ).doc;
	}

	public float score(int index) throws IOException {
		return scoreDoc( index ).score;
	}

	public Explanation explain(int index) throws IOException {
		final Explanation explanation = searcher.getSearcher().explain( preparedQuery, docId( index ) );
		timeoutManager.isTimedOut();
		return explanation;
	}

	/*
	 * return true if all requested elements are loaded
	 */
	private boolean updateTopDocs(int n) throws IOException {
		final TopDocsCollector<?> topCollector;
		Collector maybeTimeLimitingCollector; //need that because TimeLimitingCollector is not a TopDocsCollector
		//copied from IndexSearcher#search
		//we only accept indexSearcher atm which means we can copy its the Collector creation strategy
		final int maxDocs = Math.min( n, searcher.getSearcher().maxDoc() );
		final Weight weight = preparedQuery.weight( searcher.getSearcher() );
		if ( sort == null ) {
			topCollector = TopScoreDocCollector.create(maxDocs, !weight.scoresDocsOutOfOrder());
		}
		else {
			boolean fillFields = true;
			topCollector = TopFieldCollector.create(
					sort, 
					maxDocs,
        			fillFields,
					searcher.isFieldSortDoTrackScores(),
					searcher.isFieldSortDoMaxScore(),
					!weight.scoresDocsOutOfOrder()
			);
		}
		maybeTimeLimitingCollector = topCollector;
		if ( timeoutManager.isBestEffort() ) {
			final Long timeoutLeft = timeoutManager.getTimeoutLeftInMilliseconds();
			if ( timeoutLeft != null ) {
				maybeTimeLimitingCollector = new TimeLimitingCollector(topCollector, timeoutLeft );
			}
		}
		try {
			searcher.getSearcher().search(weight, filter, maybeTimeLimitingCollector);
		}
		catch ( TimeLimitingCollector.TimeExceededException e ) {
			//we have reached the time limit and stopped before the end
			//TimeoutManager.isTimedOut should be above that limit but set if for safety
			timeoutManager.forceTimedOut();
			topDocs = topCollector.topDocs();
		}
		topDocs = topCollector.topDocs();
		return timeoutManager.isTimedOut();
	}
}
