/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.impl;

import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.AnalyzerDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.NormalizerDef;
import org.hibernate.search.util.SearchException;

public interface LuceneAnalysisDefinitionCollector {

	/**
	 * Collect an analyzer definition.
	 * @param name The name of the definition to be registered.
	 * @param definition The definition to be registered.
	 * @throws SearchException if the name is already associated with a different definition.
	 */
	void collect(String name, AnalyzerDef definition);

	/**
	 * Collect a normalizer definition.
	 * @param name The name of the definition to be registered.
	 * @param definition The definition to be registered.
	 * @throws SearchException if the name is already associated with a different definition.
	 */
	void collect(String name, NormalizerDef definition);

}
