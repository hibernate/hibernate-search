/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

public interface LuceneCollectorProvider {

	/**
	 * Contribute to the Lucene collectors, making sure that the information required by this extractor are collected.
	 *
	 * @param luceneCollectorBuilder the Lucene collector builder.
	 */
	void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder);
}
