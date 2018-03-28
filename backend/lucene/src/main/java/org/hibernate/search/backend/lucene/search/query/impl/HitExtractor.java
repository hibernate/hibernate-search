/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.apache.lucene.document.Document;

public interface HitExtractor<C> {

	/**
	 * Contribute to the Lucene collectors, making sure that the information required by this extractor are collected.
	 *
	 * @param luceneCollectorBuilder the Lucene collector builder.
	 */
	void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder);

	/**
	 * Contributes to the list of fields extracted from the Lucene document. Some fields might require the extraction of
	 * other fields e.g. if the stored fields have different names.
	 *
	 * @param absoluteFieldPaths
	 */
	void contributeFields(Set<String> absoluteFieldPaths);

	/**
	 * Perform hit extraction.
	 *
	 * @param collector The hit collector, which will receive the result of the extraction.
	 * @param document The Lucene document extracted from the index.
	 */
	void extract(C collector, Document document);
}
