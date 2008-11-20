// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.search.query;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

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
	public final Searcher searcher;
	public final Filter filter;
	public final Sort sort;
	public final int totalHits;
	public TopDocs topDocs;

	public QueryHits(Searcher searcher, org.apache.lucene.search.Query preparedQuery, Filter filter, Sort sort)
			throws IOException {
		this( searcher, preparedQuery, filter, sort, DEFAULT_TOP_DOC_RETRIEVAL_SIZE );
	}

	public QueryHits(Searcher searcher, org.apache.lucene.search.Query preparedQuery, Filter filter, Sort sort,
					 Integer n )
			throws IOException {
		this.preparedQuery = preparedQuery;
		this.searcher = searcher;
		this.filter = filter;
		this.sort = sort;
		updateTopDocs( n );
		totalHits = topDocs.totalHits;
	}

	public Document doc(int index) throws IOException {
		return searcher.doc( docId( index ) );
	}

	public Document doc(int index, FieldSelector selector) throws IOException {
		return searcher.doc( docId( index ), selector );
	}

	public ScoreDoc scoreDoc(int index) throws IOException {
		if ( index >= totalHits ) {
		  throw new SearchException("Not a valid ScoreDoc index: " + index);
		}

		// TODO - Is there a better way to get more TopDocs? Get more or less?
		if ( index >= topDocs.scoreDocs.length ) {
			updateTopDocs( 2 * index );
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
		return searcher.explain( preparedQuery, docId( index ) );
	}

	private void updateTopDocs(int n) throws IOException {
		if ( sort == null ) {
			topDocs = searcher.search( preparedQuery, filter, n );
		}
		else {
			topDocs = searcher.search( preparedQuery, filter, n, sort );
		}
	}
}
