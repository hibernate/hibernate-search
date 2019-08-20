/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorExtractContext;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorKey;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class SearchProjectionExtractContext implements LuceneCollectorExtractContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;
	private final Map<Integer, Set<Integer>> nestedDocs;
	private final Map<LuceneCollectorKey<?>, Collector> collectors;

	public SearchProjectionExtractContext(IndexSearcher indexSearcher, Query luceneQuery,
			Map<Integer, Set<Integer>> nestedDocs,
			Map<LuceneCollectorKey<?>, Collector> collectors) {
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.nestedDocs = nestedDocs;
		this.collectors = collectors;
	}

	public Explanation explain(int docId) {
		try {
			return indexSearcher.explain( luceneQuery, docId );
		}
		catch (IOException e) {
			throw log.ioExceptionOnExplain( e );
		}
	}

	@Override
	public Set<Integer> getNestedDocs(int docId) {
		return nestedDocs.get( docId );
	}

	@SuppressWarnings("unchecked")
	public <C extends Collector> C getCollector(LuceneCollectorKey<C> key) {
		return (C) collectors.get( key );
	}

}
