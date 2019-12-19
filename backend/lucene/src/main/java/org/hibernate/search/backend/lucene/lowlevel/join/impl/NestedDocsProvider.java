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

	private final BitSetProducer parentFiler;
	private final Query childQuery;

	public NestedDocsProvider(String nestedDocumentPath, Query originalParentQuery) {
		this( Collections.singleton( nestedDocumentPath ), originalParentQuery );
	}

	public NestedDocsProvider(Set<String> nestedDocumentPaths, Query originalParentQuery) {
		this.parentFiler = new QueryBitSetProducer( originalParentQuery );
		this.childQuery = Queries.findChildQuery( nestedDocumentPaths, originalParentQuery );
	}

	public BitSet parentDocs(LeafReaderContext context) throws IOException {
		return parentFiler.getBitSet( context );
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
