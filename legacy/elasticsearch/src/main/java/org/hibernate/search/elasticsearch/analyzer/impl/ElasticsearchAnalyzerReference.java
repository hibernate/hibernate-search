/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;

/**
 * A reference to an {@code ElasticsearchAnalyzer}.
 *
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchAnalyzerReference extends RemoteAnalyzerReference {

	/**
	 * Register definitions that will be needed in order to add the field named {@code fieldName}
	 * to the Elasticsearch mapping.
	 *
	 * @param fieldName The name of the field for which every referenced analysis definition should be
	 * registered.
	 * @param definitionRegistry The registry to be populated (it may be empty).
	 */
	public abstract void registerDefinitions(String fieldName,
			ElasticsearchAnalysisDefinitionRegistry definitionRegistry);

	public abstract boolean isInitialized();

	/**
	 * Initialize the internals of this reference, so that enough information will be available
	 * to execute {@link #getAnalyzerName(String)} and
	 * {@link #registerDefinitions(String, ElasticsearchAnalysisDefinitionRegistry)}.
	 * @param definitionRegistry The registry holding all known analyzer definitions.
	 * @param translator An {@link ElasticsearchAnalyzerDefinitionTranslator}.
	 */
	public abstract void initialize(ElasticsearchAnalysisDefinitionRegistry definitionRegistry,
			ElasticsearchAnalyzerDefinitionTranslator translator);

	@Override
	public void close() {
		// Nothing to close in Elasticsearch analyzer references
	}

}
