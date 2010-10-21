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
