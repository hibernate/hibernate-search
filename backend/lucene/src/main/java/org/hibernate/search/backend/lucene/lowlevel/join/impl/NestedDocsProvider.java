/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.join.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.search.ConjunctionUtils;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.util.BitSet;

/**
 * Provides various representations of nested docs for a pre-defined nested document path.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.IndexFieldData.Nested} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public class NestedDocsProvider {

	private final BitSetProducer parentFilter;
	private final Query childQuery;

	public NestedDocsProvider(String parentDocumentPath, String nestedDocumentPath) {
		this( parentDocumentPath, Collections.singleton( nestedDocumentPath ), null );
	}

	public NestedDocsProvider(String nestedDocumentPath, Query nestedFilter) {
		this( null, Collections.singleton( nestedDocumentPath ), nestedFilter );
	}

	public NestedDocsProvider(Set<String> nestedDocumentPaths) {
		this( null, nestedDocumentPaths, null );
	}

	public NestedDocsProvider(String parentDocumentPath, Set<String> nestedDocumentPaths, Query nestedFilter) {
		Query parentsFilterQuery = Queries.parentsFilterQuery( parentDocumentPath );
		// Note: this filter should include *all* parents, not just the matched ones.
		// Otherwise we will not "see" non-matched parents,
		// and we will consider its matching children as children of the next matching parent.
		this.parentFilter = new QueryBitSetProducer( parentsFilterQuery );
		this.childQuery = Queries.childDocumentsQuery( nestedDocumentPaths, nestedFilter );
	}

	public ChildDocIds childDocs(LeafReaderContext context, DocIdSetIterator childFilter) throws IOException {
		final IndexReaderContext topLevelCtx = ReaderUtil.getTopLevelContext( context );
		// See HSEARCH-4514; ideally we would use the calling searcher here,
		// but it's not easy to pass it to this method
		// and it hasn't been proven that creating a new searcher has a performance impact.
		IndexSearcher indexSearcher = new IndexSearcher( topLevelCtx );
		Weight childDocsWeight = childDocsWeight( indexSearcher );
		return childDocs( childDocsWeight, context, childFilter );
	}

	public ChildDocIds childDocs(Weight childDocsWeight, LeafReaderContext context,
			DocIdSetIterator childFilter)
			throws IOException {
		BitSet parentDocs = parentFilter.getBitSet( context );
		if ( parentDocs == null ) {
			return null;
		}

		Scorer s = childDocsWeight.scorer( context );
		DocIdSetIterator childDocs = s == null ? null : s.iterator();
		if ( childDocs == null ) {
			return null;
		}

		if ( childFilter != null ) {
			childDocs = ConjunctionUtils.intersectIterators( Arrays.asList( childDocs, childFilter ) );
		}

		return new ChildDocIds( parentDocs, childDocs );
	}

	public Weight childDocsWeight(IndexSearcher indexSearcher) throws IOException {
		return indexSearcher.createWeight( indexSearcher.rewrite( childQuery ), ScoreMode.COMPLETE_NO_SCORES, 1f );
	}

}
