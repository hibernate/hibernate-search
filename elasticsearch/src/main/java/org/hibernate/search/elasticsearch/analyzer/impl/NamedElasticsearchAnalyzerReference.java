/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistryPopulator;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an Elasticsearch analyzer that also provides a name for this analyzer.
 * <p>
 * Such a reference may initially only contain the analyzer name, but in this case
 * it will be fully initialized with the actual analyzer later.
 *
 * @author Yoann Rodiere
 */
public class NamedElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	protected final String name;

	private ElasticsearchAnalysisDefinitionRegistryPopulator definitionRegistryPopulator;

	public NamedElasticsearchAnalyzerReference(String name) {
		this.name = name;
	}

	@Override
	public String getAnalyzerName(String fieldName) {
		return name;
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return false;
	}

	@Override
	public void registerDefinitions(String fieldName, ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		if ( definitionRegistryPopulator == null ) {
			throw LOG.lazyRemoteAnalyzerReferenceNotInitialized( this );
		}
		definitionRegistryPopulator.populate( definitionRegistry );
	}

	@Override
	public boolean isInitialized() {
		return definitionRegistryPopulator != null;
	}

	@Override
	public void initialize(ElasticsearchAnalysisDefinitionRegistry definitionRegistry, ElasticsearchAnalyzerDefinitionTranslator translator) {
		if ( this.definitionRegistryPopulator != null ) {
			throw new AssertionFailure( "A named analyzer reference has been initialized more than once: " + this );
		}
		this.definitionRegistryPopulator = createRegistryPopulator( definitionRegistry );
	}

	protected ElasticsearchAnalysisDefinitionRegistryPopulator createRegistryPopulator(ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		AnalyzerDefinition analyzerDefinition = definitionRegistry.getAnalyzerDefinition( name );
		if ( analyzerDefinition == null ) {
			return (r) -> { }; // No-op
		}

		String tokenizerName = analyzerDefinition.getTokenizer();
		TokenizerDefinition tokenizerDefinition = definitionRegistry.getTokenizerDefinition( tokenizerName );

		Map<String, TokenFilterDefinition> tokenFilters =
				collectDefinitions( definitionRegistry::getTokenFilterDefinition, analyzerDefinition.getTokenFilters() );

		Map<String, CharFilterDefinition> charFilters =
				collectDefinitions( definitionRegistry::getCharFilterDefinition, analyzerDefinition.getCharFilters() );

		return new SimpleElasticsearchAnalysisDefinitionRegistryPopulator(
				name, analyzerDefinition,
				tokenizerName, tokenizerDefinition,
				charFilters, tokenFilters );
	}

	protected final <T> Map<String, T> collectDefinitions(Function<String, T> registry, Collection<String> names) {
		Map<String, T> result = new TreeMap<>();
		if ( names != null ) {
			for ( String name : names ) {
				T definition = registry.apply( name );
				if ( definition != null ) { // Ignore missing definitions: they may be already available on the server
					result.put( name, definition );
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( name );
		sb.append( "," );
		sb.append( definitionRegistryPopulator );
		sb.append( ">" );
		return sb.toString();
	}
}
