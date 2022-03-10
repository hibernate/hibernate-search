/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.search.extraction.impl.CollectorSet;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class ProjectionExtractContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;
	private final CollectorSet collectors;

	public ProjectionExtractContext(IndexSearcher indexSearcher, Query luceneQuery,
			CollectorSet collectors) {
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.collectors = collectors;
	}

	public Explanation explain(int docId) {
		try {
			return indexSearcher.explain( luceneQuery, docId );
		}
		catch (IOException e) {
			throw log.ioExceptionOnExplain( e.getMessage(), e );
		}
	}

	public <C extends Collector> C getCollector(CollectorKey<C> key) {
		return collectors == null ? null : collectors.get( key );
	}

}
