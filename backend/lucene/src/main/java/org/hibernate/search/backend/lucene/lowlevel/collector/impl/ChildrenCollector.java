/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Weight;

public class ChildrenCollector extends SimpleCollector {

	private final NestedDocsProvider nestedDocsProvider;
	private final Weight childrenWeight;

	private int currentLeafDocBase;
	private int currentLeafLastSeenParentDoc;
	private DocIdSetIterator currentLeafChildDocs;

	private final Map<Integer, Set<Integer>> children = new HashMap<>();

	public ChildrenCollector(IndexSearcher indexSearcher, NestedDocsProvider nestedDocsProvider) throws IOException {
		this.childrenWeight = nestedDocsProvider.childDocsWeight( indexSearcher );
		this.nestedDocsProvider = nestedDocsProvider;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "ChildrenCollector{" );
		sb.append( "children=" ).append( children );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public void collect(int parentDoc) throws IOException {
		if ( currentLeafChildDocs == null ) {
			return; // No children in this leaf
		}

		if ( parentDoc < currentLeafLastSeenParentDoc ) {
			throw new AssertionFailure( "Collector.collect called in unexpected order" );
		}

		final int firstChildDoc;
		if ( currentLeafChildDocs.docID() > currentLeafLastSeenParentDoc ) {
			firstChildDoc = currentLeafChildDocs.docID();
		}
		else {
			firstChildDoc = currentLeafChildDocs.advance( currentLeafLastSeenParentDoc + 1 );
		}
		currentLeafLastSeenParentDoc = parentDoc;

		if ( firstChildDoc > parentDoc ) {
			// No child
			return;
		}

		Set<Integer> childrenOfThisDoc = new HashSet<>();
		children.put( parentDoc, childrenOfThisDoc );

		for ( int childDoc = firstChildDoc; childDoc < parentDoc; childDoc = currentLeafChildDocs.nextDoc() ) {
			childrenOfThisDoc.add( currentLeafDocBase + childDoc );
		}
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public Map<Integer, Set<Integer>> getChildren() {
		return children;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.currentLeafDocBase = context.docBase;
		this.currentLeafLastSeenParentDoc = -1;

		this.currentLeafChildDocs = nestedDocsProvider.childDocs( childrenWeight, context );
	}
}
