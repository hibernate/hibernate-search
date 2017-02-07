/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistryBuilderImpl;
import org.hibernate.search.elasticsearch.analyzer.definition.spi.ElasticsearchAnalysisDefinitionProvider;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerStrategy implements AnalyzerStrategy {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ServiceManager serviceManager;

	private final SearchConfiguration cfg;

	public ElasticsearchAnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		this.serviceManager = serviceManager;
		this.cfg = cfg;
	}

	private SimpleElasticsearchAnalysisDefinitionRegistry createDefaultDefinitionRegistry() {
		ElasticsearchAnalysisDefinitionRegistryBuilderImpl builder =
				new ElasticsearchAnalysisDefinitionRegistryBuilderImpl();

		String providerClassName = cfg.getProperty( ElasticsearchEnvironment.ANALYZER_DEFINITION_PROVIDER );
		if ( providerClassName != null ) {
			ElasticsearchAnalysisDefinitionProvider provider;
			try {
				Class<?> providerClazz = ClassLoaderHelper.classForName( providerClassName, serviceManager );
				provider = (ElasticsearchAnalysisDefinitionProvider) ReflectionHelper.createInstance( providerClazz, true );
			}
			catch (RuntimeException e) {
				throw LOG.invalidElasticsearchAnalyzerDefinitionProvider( providerClassName, e );
			}
			try {
				provider.register( builder );
			}
			catch (SearchException e) { // Do not wrap our own exceptions (from the builder, for instance)
				throw e;
			}
			catch (RuntimeException e) { // Do wrap any other exception
				throw LOG.invalidLuceneAnalyzerDefinitionProvider( providerClassName, e );
			}
		}

		return builder.build();
	}

	@Override
	public ElasticsearchAnalyzerReference createDefaultAnalyzerReference() {
		return new NamedElasticsearchAnalyzerReference( "default" );
	}

	@Override
	public ElasticsearchAnalyzerReference createPassThroughAnalyzerReference() {
		return new NamedElasticsearchAnalyzerReference( "keyword" );
	}

	@Override
	public NamedElasticsearchAnalyzerReference createNamedAnalyzerReference(String name) {
		return new NamedElasticsearchAnalyzerReference( name );
	}

	@Override
	public ElasticsearchAnalyzerReference createLuceneClassAnalyzerReference(Class<?> analyzerClass) {
		return new LuceneClassElasticsearchAnalyzerReference( analyzerClass );
	}

	@Override
	public Map<String, AnalyzerReference> initializeAnalyzerReferences(
			Collection<AnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		try ( ServiceReference<ElasticsearchAnalyzerDefinitionTranslator> translatorReference =
				serviceManager.requestReference( ElasticsearchAnalyzerDefinitionTranslator.class ) ) {
			ElasticsearchAnalyzerDefinitionTranslator translator = translatorReference.get();

			// First, create a registry containing all relevant definitions
			/*
			 * Recreate the default definitions for each call,
			 * so that the definition providers can add new definitions between two SearchFactory increments.
			 */
			SimpleElasticsearchAnalysisDefinitionRegistry defaultDefinitionRegistry = createDefaultDefinitionRegistry();
			ElasticsearchAnalysisDefinitionRegistry definitionRegistry =
					createDefinitionRegistry( references, defaultDefinitionRegistry, analyzerDefinitions, translator);

			Set<String> existingNamedReferences = new HashSet<>();

			// When all definitions are known and translated, actually initialize the references
			for ( AnalyzerReference reference : references ) {
				if ( reference.is( NamedElasticsearchAnalyzerReference.class ) ) {
					NamedElasticsearchAnalyzerReference namedReference = reference.unwrap( NamedElasticsearchAnalyzerReference.class );
					if ( !namedReference.isInitialized() ) {
						initializeNamedReference( namedReference, definitionRegistry );
					}
					existingNamedReferences.add( namedReference.getAnalyzerName() );
				}
				else if ( reference.is( LuceneClassElasticsearchAnalyzerReference.class ) ) {
					LuceneClassElasticsearchAnalyzerReference luceneClassReference = reference.unwrap( LuceneClassElasticsearchAnalyzerReference.class );
					if ( !luceneClassReference.isInitialized() ) {
						initializeLuceneClassReference( luceneClassReference, translator );
					}
				}
				else if ( reference.is( ScopedElasticsearchAnalyzerReference.class ) ) {
					ScopedElasticsearchAnalyzerReference scopedReference = reference.unwrap( ScopedElasticsearchAnalyzerReference.class );
					if ( !scopedReference.isInitialized() ) {
						scopedReference.initialize();
					}
				}
			}

			/*
			 * Finally, create additional references for default definitions that
			 * haven't any matching reference, so that they will be available when querying.
			 * We don't do that for @AnalyzerDefs because they may not all be related to Elasticsearch,
			 * and they might even not be translatable to an Elasticsearch definition.
			 */
			Map<String, AnalyzerReference> additionalNamedReferences = new HashMap<>();
			for ( String defaultAnalyzerName : defaultDefinitionRegistry.getAnalyzerDefinitions().keySet() ) {
				if ( !existingNamedReferences.contains( defaultAnalyzerName ) ) {
					NamedElasticsearchAnalyzerReference reference = createNamedAnalyzerReference( defaultAnalyzerName );
					initializeNamedReference( reference, definitionRegistry );
					additionalNamedReferences.put( defaultAnalyzerName, reference );
				}
			}

			return additionalNamedReferences;
		}
	}

	private ElasticsearchAnalysisDefinitionRegistry createDefinitionRegistry(Collection<AnalyzerReference> references,
			ElasticsearchAnalysisDefinitionRegistry defaultDefinitionRegistry,
			Map<String, AnalyzerDef> analyzerDefinitions, ElasticsearchAnalyzerDefinitionTranslator translator) {
		/*
		 * Make default definitions accessible in the final definition registry.
		 * This final registry has two scopes:
		 *  - the "local" scope, which contains every definition gathered from pre-existing references (see below)
		 *    and definitions from the mapping
		 *  - the "default"/"global" scope, which contains definitions from the default registry (see above).
		 *
		 * When fetching definitions, the "local" scope takes precedence over the "default"/"global" scope.
		 *
		 * Note that thanks to this setup, changes to pre-existing default definitions are ignored
		 * if the definitions were already used in pre-existing references.
		 */
		ElasticsearchAnalysisDefinitionRegistry definitionRegistry =
				new ChainingElasticsearchAnalysisDefinitionRegistry( defaultDefinitionRegistry );

		/*
		 * First, populate the registry with definitions from already initialized references.
		 *
		 * Those can occur if we are currently "incrementing"
		 * on top of a previous version of the search factory.
		 * In this case, we want to add previous definitions to the registry,
		 * so as to check that we don't have conflicts
		 * between the previous definitions and some new ones.
		 */
		for ( AnalyzerReference reference : references ) {
			if ( reference.is( NamedElasticsearchAnalyzerReference.class ) ) {
				NamedElasticsearchAnalyzerReference namedReference = reference.unwrap( NamedElasticsearchAnalyzerReference.class );
				if ( namedReference.isInitialized() ) {
					// Note: these analyzers don't handle scope, we don't care about the field name
					namedReference.getAnalyzer().registerDefinitions( definitionRegistry, null );
				}
			}
		}

		/*
		 * Once the registry has been populated with pre-existing definitions,
		 * add the new definitions as needed, throwing exceptions if there are conflicts.
		 *
		 * Note that we populate the registry only with the definitions we actually use.
		 * That's because some @AnalyzerDef's cannot be translated,
		 * and users may decide to add such definitions anyway because they need them
		 * for entities indexed in an embedded Lucene instance (not ES).
		 */
		TranslatingElasticsearchAnalyzerDefinitionRegistryPopulator translatingPopulator =
				new TranslatingElasticsearchAnalyzerDefinitionRegistryPopulator( definitionRegistry, translator );

		for ( AnalyzerReference reference : references ) {
			if ( reference.is( NamedElasticsearchAnalyzerReference.class ) ) {
				NamedElasticsearchAnalyzerReference namedReference = reference.unwrap( NamedElasticsearchAnalyzerReference.class );
				if ( !namedReference.isInitialized() ) {
					String name = namedReference.getAnalyzerName();
					AnalyzerDef hibernateSearchAnalyzerDef = analyzerDefinitions.get( name );
					if ( hibernateSearchAnalyzerDef != null ) {
						translatingPopulator.registerAnalyzerDef( hibernateSearchAnalyzerDef );
					}
				}
			}
		}

		return definitionRegistry;
	}

	private void initializeNamedReference(NamedElasticsearchAnalyzerReference analyzerReference,
			ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		String name = analyzerReference.getAnalyzerName();

		ElasticsearchAnalyzer analyzer = createAnalyzer( definitionRegistry, name );

		analyzerReference.initialize( analyzer );
	}

	private void initializeLuceneClassReference(LuceneClassElasticsearchAnalyzerReference analyzerReference,
			ElasticsearchAnalyzerDefinitionTranslator translator) {
		Class<?> clazz = analyzerReference.getLuceneClass();

		String name = translator.translate( clazz );

		ElasticsearchAnalyzer analyzer = new UndefinedElasticsearchAnalyzerImpl( name );

		analyzerReference.initialize( name, analyzer );
	}

	@Override
	public ScopedElasticsearchAnalyzerReference.Builder buildScopedAnalyzerReference(AnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedElasticsearchAnalyzerReference.DeferredInitializationBuilder(
				initialGlobalAnalyzerReference.unwrap( ElasticsearchAnalyzerReference.class ),
				Collections.<String, ElasticsearchAnalyzerReference>emptyMap()
				);
	}

	private ElasticsearchAnalyzer createAnalyzer(ElasticsearchAnalysisDefinitionRegistry definitionRegistry, String analyzerName) {
		AnalyzerDefinition analyzerDefinition = definitionRegistry.getAnalyzerDefinition( analyzerName );
		if ( analyzerDefinition == null ) {
			return new UndefinedElasticsearchAnalyzerImpl( analyzerName );
		}

		String tokenizerName = analyzerDefinition.getTokenizer();
		TokenizerDefinition tokenizerDefinition = definitionRegistry.getTokenizerDefinition( tokenizerName );

		Map<String, TokenFilterDefinition> tokenFilters = new TreeMap<>();
		if ( analyzerDefinition.getTokenFilters() != null ) {
			for ( String name : analyzerDefinition.getTokenFilters() ) {
				TokenFilterDefinition definition = definitionRegistry.getTokenFilterDefinition( name );
				if ( definition != null ) { // Ignore missing definitions: they may be already available on the server
					tokenFilters.put( name, definition );
				}
			}
		}

		Map<String, CharFilterDefinition> charFilters = new TreeMap<>();
		if ( analyzerDefinition.getCharFilters() != null ) {
			for ( String name : analyzerDefinition.getCharFilters() ) {
				CharFilterDefinition definition = definitionRegistry.getCharFilterDefinition( name );
				if ( definition != null ) { // Ignore missing definitions: they may be already available on the server
					charFilters.put( name, definition );
				}
			}
		}

		return new CustomElasticsearchAnalyzerImpl(
				analyzerName, analyzerDefinition,
				tokenizerName, tokenizerDefinition,
				charFilters, tokenFilters );
	}

}
