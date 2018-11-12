/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExecutionContext;

public interface HitExtractor<C> extends LuceneCollectorProvider {

	/**
	 * Contributes to the list of fields extracted from the Lucene document. Some fields might require the extraction of
	 * other fields e.g. if the stored fields have different names.
	 *
	 * @param absoluteFieldPaths The set of absolute field paths contributed.
	 */
	default void contributeFields(Set<String> absoluteFieldPaths) {
	}

	/**
	 * Perform hit extraction.
	 * @param collector The hit collector, which will receive the result of the extraction.
	 * @param documentResult A wrapper on top of the Lucene document extracted from the index.
	 * @param context An execution context for the search projections.
	 */
	void extract(C collector, LuceneResult documentResult, SearchProjectionExecutionContext context);
}
