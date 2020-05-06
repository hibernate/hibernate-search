/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index;

import org.hibernate.search.engine.backend.index.IndexManager;

import org.apache.lucene.analysis.Analyzer;

public interface LuceneIndexManager extends IndexManager {

	/**
	 * @return The analyzer used when indexing.
	 * This analyzer behaves differently for each field,
	 * delegating to the analyzer configured in the mapping.
	 */
	Analyzer indexingAnalyzer();

	/**
	 * @return The analyzer used in search queries.
	 * This analyzer behaves differently for each field,
	 * delegating to the analyzer configured in the mapping.
	 */
	Analyzer searchAnalyzer();

}
