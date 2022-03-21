/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.join.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
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
 * Provides the {@link #parentDocs(LeafReaderContext)} and {@link #childDocs(LeafReaderContext)},
 * relatives to the current sort.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.IndexFieldData.Nested} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public class NestedDocsProvider {

	private final BitSetProducer parentFilter;
	private final Query childQuery;

	public NestedDocsProvider(String nestedDocumentPath) {
		this( Collections.singleton( nestedDocumentPath ), null );
	}

	public NestedDocsProvider(String nestedDocumentPath, Query nestedFilter) {
		this( Collections.singleton( nestedDocumentPath ), nestedFilter );
	}

	public NestedDocsProvider(Set<String> nestedDocumentPaths) {
		this( nestedDocumentPaths, null );
	}

	public NestedDocsProvider(Set<String> nestedDocumentPaths, Query nestedFilter) {
		Query parentsFilterQuery = Queries.parentsFilterQuery( null );
		// Note: this filter should include *all* parents, not just the matched ones.
		// Otherwise we will not "see" non-matched parents,
		// and we will consider its matching children as children of the next matching parent.
		this.parentFilter = new QueryBitSetProducer( parentsFilterQuery );
		this.childQuery = Queries.childDocumentsQuery( nestedDocumentPaths, nestedFilter );
	}

	public BitSet parentDocs(LeafReaderContext context) throws IOException {
		return parentFilter.getBitSet( context );
	}

	public DocIdSetIterator childDocs(LeafReaderContext context) throws IOException {
		final IndexReaderContext topLevelCtx = ReaderUtil.getTopLevelContext( context );

		// Maybe we can cache on shard-base. See Elasticsearch code.
		IndexSearcher indexSearcher = new IndexSearcher( topLevelCtx );

		Weight weight = childDocsWeight( indexSearcher );
		return childDocs( weight, context );
	}

	public Weight childDocsWeight(IndexSearcher indexSearcher) throws IOException {
		return indexSearcher.createWeight( indexSearcher.rewrite( childQuery ), ScoreMode.COMPLETE_NO_SCORES, 1f );
	}

	public DocIdSetIterator childDocs(Weight weight, LeafReaderContext context) throws IOException {
		Scorer s = weight.scorer( context );
		return s == null ? null : s.iterator();
	}
}
