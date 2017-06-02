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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisDefinitionProvider;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ChainingElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistryBuilderImpl;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.NamespaceMergingElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.SimpleElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * An {@link AnalyzerStrategy} for Elasticsearch 2 to 5.1.
 *
 * @author Yoann Rodiere
 */
public class Elasticsearch2AnalyzerStrategy implements AnalyzerStrategy {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ServiceManager serviceManager;

	private final SimpleElasticsearchAnalysisDefinitionRegistry defaultDefinitionRegistry;

	public Elasticsearch2AnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		this.serviceManager = serviceManager;
		/*
		 * Make sure to re-create the default definition registry with each newly instantiated strategy,
		 * so that the definition providers can add new definitions between two SearchFactory increments.
		 * Caching those in a Service, for instance, would prevent that.
		 */
		this.defaultDefinitionRegistry = createDefaultDefinitionRegistry(cfg);
	}

	private SimpleElasticsearchAnalysisDefinitionRegistry createDefaultDefinitionRegistry( SearchConfiguration cfg ) {
		ElasticsearchAnalysisDefinitionRegistryBuilderImpl builder =
				new ElasticsearchAnalysisDefinitionRegistryBuilderImpl();

		String providerClassName = cfg.getProperty( ElasticsearchEnvironment.ANALYSIS_DEFINITION_PROVIDER );
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

		SimpleElasticsearchAnalysisDefinitionRegistry registry = new SimpleElasticsearchAnalysisDefinitionRegistry();
		builder.build( wrapForAdditions( registry ) );
		return registry;
	}

	protected ElasticsearchAnalysisDefinitionRegistry wrapForAdditions(ElasticsearchAnalysisDefinitionRegistry registry) {
		return new NamespaceMergingElasticsearchAnalysisDefinitionRegistry( registry );
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
	public Map<String, AnalyzerReference> createProvidedAnalyzerReferences() {
		Map<String, AnalyzerReference> references = new HashMap<>();
		for ( String defaultAnalyzerName : defaultDefinitionRegistry.getAnalyzerDefinitions().keySet() ) {
			NamedElasticsearchAnalyzerReference reference = createNamedAnalyzerReference( defaultAnalyzerName );
			references.put( defaultAnalyzerName, reference );
		}
		return references;
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
	public Map<String, AnalyzerReference> createProvidedNormalizerReferences() {
		Map<String, AnalyzerReference> references = new HashMap<>();
		for ( String name : defaultDefinitionRegistry.getNormalizerDefinitions().keySet() ) {
			AnalyzerReference reference = createNamedNormalizerReference( name );
			references.put( name, reference );
		}
		return references;
	}

	@Override
	public ElasticsearchAnalyzerReference createNamedNormalizerReference(String name) {
		return new NamedElasticsearch2NormalizerReference( name );
	}

	@Override
	public ElasticsearchAnalyzerReference createLuceneClassNormalizerReference(Class<?> analyzerClass) {
		throw LOG.cannotUseNormalizerImpl( analyzerClass );
	}

	@Override
	public void initializeReferences(Collection<AnalyzerReference> analyzerReferences, Map<String, AnalyzerDef> mappingAnalyzerDefinitions,
			Collection<AnalyzerReference> normalizerReferences, Map<String, NormalizerDef> mappingNormalizerDefinitions) {
		try ( ServiceReference<ElasticsearchAnalyzerDefinitionTranslator> translatorReference =
				serviceManager.requestReference( ElasticsearchAnalyzerDefinitionTranslator.class ) ) {
			ElasticsearchAnalyzerDefinitionTranslator translator = translatorReference.get();

			// First, create a registry containing all relevant definitions
			ElasticsearchAnalysisDefinitionRegistry definitionRegistry =
					createDefinitionRegistry( analyzerReferences, mappingAnalyzerDefinitions,
							normalizerReferences, mappingNormalizerDefinitions,
							defaultDefinitionRegistry, translator);

			// When all definitions are known and translated, actually initialize the references
			Stream.concat( analyzerReferences.stream(), normalizerReferences.stream() )
					.map( this::getUninitializedReference )
					.filter( Objects::nonNull )
					.forEach( r -> r.initialize( definitionRegistry, translator ) );
		}
	}

	private ElasticsearchAnalysisDefinitionRegistry createDefinitionRegistry(
			Collection<AnalyzerReference> analyzerReferences,
			Map<String, AnalyzerDef> analyzerDefinitions,
			Collection<AnalyzerReference> normalizerReferences,
			Map<String, NormalizerDef> normalizerDefinitions,
			ElasticsearchAnalysisDefinitionRegistry defaultDefinitionRegistry,
			ElasticsearchAnalyzerDefinitionTranslator translator) {
		ElasticsearchAnalysisDefinitionRegistry localDefinitionRegistry =
				new SimpleElasticsearchAnalysisDefinitionRegistry();

		/*
		 * Make default definitions accessible in the final definition registry.
		 * This final registry has two scopes:
		 *  - the "local" scope, which contains every definition gathered from pre-existing references (see below)
		 *    and definitions from the mapping
		 *  - the "default"/"global" scope, which contains definitions from the default registry (see above).
		 *
		 * When fetching definitions, the "local" scope takes precedence over the "default"/"global" scope.
		 *
		 * Note that thanks to this setup, changes to pre-existing default definitions are ignored.
		 */
		ElasticsearchAnalysisDefinitionRegistry chainingRegistry =
				new ChainingElasticsearchAnalysisDefinitionRegistry(
						localDefinitionRegistry, defaultDefinitionRegistry );

		ElasticsearchAnalysisDefinitionRegistry definitionRegistry = wrapForAdditions( chainingRegistry );

		/*
		 * First, populate the registry with definitions from already initialized references.
		 *
		 * Those can occur if we are currently "incrementing"
		 * on top of a previous version of the search factory.
		 * In this case, we want to add previous definitions to the registry,
		 * so as to check that we don't have conflicts
		 * between the previous definitions and some new ones.
		 *
		 * This is especially necessary to handle cases where normalizers are translated
		 * to analyzers under the hood (ES 5.1 and below), which means normalizer definitions
		 * and analyzer definitions will share the same namespace even if references don't.
		 * See HSEARCH-2730 and why it was rejected for details.
		 */
		Stream.concat( analyzerReferences.stream(), normalizerReferences.stream() )
				.map( this::getInitializedNamedReference )
				.filter( Objects::nonNull )
				// Note: these references don't handle scope, we don't care about the field name
				.forEach( r -> r.registerDefinitions( null, definitionRegistry ) );

		/*
		 * Once the registry has been populated with pre-existing definitions,
		 * add the new definitions as needed, throwing exceptions if there are conflicts.
		 *
		 * Note that we populate the registry only with the definitions we actually use.
		 * That's because some @AnalyzerDef's cannot be translated,
		 * and users may decide to add such definitions anyway because they need them
		 * for entities indexed in an embedded Lucene instance (not ES).
		 */
		TranslatingElasticsearchAnalysisDefinitionRegistryPopulator translatingPopulator =
				new TranslatingElasticsearchAnalysisDefinitionRegistryPopulator( definitionRegistry, translator );

		analyzerReferences.stream()
				.map( this::getUninitializedNamedReference )
				.filter( Objects::nonNull )
				// Note: these references don't handle scope, we don't care about the field name
				.map( r -> r.getAnalyzerName( null ) )
				.map( analyzerDefinitions::get )
				.filter( Objects::nonNull )
				.forEach( translatingPopulator::registerAnalyzerDef );

		normalizerReferences.stream()
				.map( this::getUninitializedNamedReference )
				.filter( Objects::nonNull )
				// Note: these references don't handle scope, we don't care about the field name
				.map( r -> r.getAnalyzerName( null ) )
				.map( normalizerDefinitions::get )
				.filter( Objects::nonNull )
				.forEach( translatingPopulator::registerNormalizerDef );

		return definitionRegistry;
	}

	private NamedElasticsearchAnalyzerReference getInitializedNamedReference(AnalyzerReference reference) {
		if ( reference.is( NamedElasticsearchAnalyzerReference.class ) ) {
			NamedElasticsearchAnalyzerReference esReference = reference.unwrap( NamedElasticsearchAnalyzerReference.class );
			if ( esReference.isInitialized() ) {
				return esReference;
			}
		}
		return null;
	}

	private NamedElasticsearchAnalyzerReference getUninitializedNamedReference(AnalyzerReference reference) {
		ElasticsearchAnalyzerReference esReference = getUninitializedReference( reference );
		if ( esReference != null && esReference.is( NamedElasticsearchAnalyzerReference.class ) ) {
			return esReference.unwrap( NamedElasticsearchAnalyzerReference.class );
		}
		return null;
	}

	private ElasticsearchAnalyzerReference getUninitializedReference(AnalyzerReference reference) {
		if ( reference.is( ElasticsearchAnalyzerReference.class ) ) {
			ElasticsearchAnalyzerReference esReference = reference.unwrap( ElasticsearchAnalyzerReference.class );
			if ( !esReference.isInitialized() ) {
				return esReference;
			}
		}
		return null;
	}

	@Override
	public ScopedElasticsearchAnalyzerReference.Builder buildScopedAnalyzerReference(AnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedElasticsearchAnalyzerReference.Builder(
				initialGlobalAnalyzerReference.unwrap( ElasticsearchAnalyzerReference.class ),
				Collections.<String, ElasticsearchAnalyzerReference>emptyMap()
				);
	}

}
