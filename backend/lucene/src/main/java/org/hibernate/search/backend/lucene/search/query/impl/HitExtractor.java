/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

public interface HitExtractor<C> {

	/**
	 * Contribute to the Lucene collectors, making sure that the information required by this extractor are collected.
	 *
	 * @param luceneCollectorBuilder the Lucene collector builder.
	 */
	void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder);

	/**
	 * Perform hit extraction.
	 *
	 * @param collector The hit collector, which will receive the result of the extraction.
	 * @param indexSearcher The Lucene index searcher.
	 * @param scoreDoc The document id and score of the hit.
	 * @throws IOException if Lucene threw an IOException when extracting the information.
	 */
	void extract(C collector, IndexSearcher indexSearcher, ScoreDoc scoreDoc) throws IOException;
}
