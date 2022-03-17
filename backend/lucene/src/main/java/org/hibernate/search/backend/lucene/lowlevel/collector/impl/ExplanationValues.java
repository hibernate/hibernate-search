/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public final class ExplanationValues implements Values<Explanation> {

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;

	private int currentLeafDocBase;

	public ExplanationValues(TopDocsDataCollectorExecutionContext context) {
		this.indexSearcher = context.getIndexSearcher();
		this.luceneQuery = context.executedQuery();
	}

	@Override
	public void context(LeafReaderContext context) throws IOException {
		this.currentLeafDocBase = context.docBase;
	}

	@Override
	public Explanation get(int doc) throws IOException {
		return indexSearcher.explain( luceneQuery, currentLeafDocBase + doc );
	}
}
