/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.TokenizerDefinition;
import org.hibernate.search.util.common.SearchException;

public interface ElasticsearchAnalysisDefinitionCollector {

	/**
	 * Collect an analyzer definition.
	 * @param name The name of the definition to be registered.
	 * @param definition The definition to be registered.
	 * @throws SearchException if the name is already associated with a different definition.
	 */
	void collect(String name, AnalyzerDefinition definition);

	/**
	 * Collect a normalizer definition.
	 * @param name The name of the definition to be registered.
	 * @param definition The definition to be registered.
	 * @throws SearchException if the name is already associated with a different definition.
	 */
	void collect(String name, NormalizerDefinition definition);

	/**
	 * Collect a tokenizer definition.
	 * @param name The name of the definition to be registered.
	 * @param definition The definition to be registered.
	 * @throws SearchException if the name is already associated with a different definition.
	 */
	void collect(String name, TokenizerDefinition definition);

	/**
	 * Collect a token filter definition.
	 * @param name The name of the definition to be registered.
	 * @param definition The definition to be registered.
	 * @throws SearchException if the name is already associated with a different definition.
	 */
	void collect(String name, TokenFilterDefinition definition);

	/**
	 * Collect a char filter definition.
	 * @param name The name of the definition to be registered.
	 * @param definition The definition to be registered.
	 * @throws SearchException if the name is already associated with a different definition.
	 */
	void collect(String name, CharFilterDefinition definition);

}
