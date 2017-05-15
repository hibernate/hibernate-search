/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionProvider;
import org.hibernate.search.analyzer.definition.impl.LuceneAnalyzerDefinitionRegistryBuilderImpl;
import org.hibernate.search.analyzer.definition.spi.LuceneAnalyzerDefinitionSourceService;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class LuceneEmbeddedAnalyzerStrategy implements AnalyzerStrategy {

	private static final Log log = LoggerFactory.make();

	private final ServiceManager serviceManager;

	private final SearchConfiguration cfg;

	private final Version luceneMatchVersion;

	private final Map<String, AnalyzerDef> defaultAnalyzerDefinitions;

	public LuceneEmbeddedAnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		this.serviceManager = serviceManager;
		this.cfg = cfg;
		this.luceneMatchVersion = getLuceneMatchVersion( cfg );
		/*
		 * Make sure to re-create the default definitions with each newly instantiated strategy,
		 * so that the definition providers can add new definitions between two SearchFactory increments.
		 * Caching those in a Service, for instance, would prevent that.
		 */
		this.defaultAnalyzerDefinitions = createDefaultAnalyzerDefinitions( cfg );
	}

	private Version getLuceneMatchVersion(SearchConfiguration cfg) {
		final Version version;
		String tmp = cfg.getProperty( Environment.LUCENE_MATCH_VERSION );
		if ( StringHelper.isEmpty( tmp ) ) {
			log.recommendConfiguringLuceneVersion();
			version = Environment.DEFAULT_LUCENE_MATCH_VERSION;
		}
		else {
			try {
				version = Version.parseLeniently( tmp );
				if ( log.isDebugEnabled() ) {
					log.debug( "Setting Lucene compatibility to Version " + version );
				}
			}
			catch (IllegalArgumentException e) {
				throw log.illegalLuceneVersionFormat( tmp, e.getMessage() );
			}
			catch (ParseException e) {
				throw log.illegalLuceneVersionFormat( tmp, e.getMessage() );
			}
		}
		return version;
	}

	private Map<String, AnalyzerDef> createDefaultAnalyzerDefinitions(SearchConfiguration cfg) {
		final LuceneAnalyzerDefinitionProvider definitionsProvider = getLuceneAnalyzerDefinitionProvider();
		LuceneAnalyzerDefinitionRegistryBuilderImpl builder = new LuceneAnalyzerDefinitionRegistryBuilderImpl();
		if ( definitionsProvider != null ) {
			try {
				definitionsProvider.register( builder );
			}
			catch (SearchException e) { // Do not wrap our own exceptions (from the builder, for instance)
				throw e;
			}
			catch (RuntimeException e) { // Do wrap any other exception
				throw log.invalidLuceneAnalyzerDefinitionProvider( definitionsProvider.getClass().getName(), e );
			}
		}
		return builder.build();
	}

	private LuceneAnalyzerDefinitionProvider getLuceneAnalyzerDefinitionProvider() {
		// Uses a Service so that integrators can inject alternative Lucene Analyzer definition providers
		try ( ServiceReference<LuceneAnalyzerDefinitionSourceService> serviceRef = serviceManager.requestReference( LuceneAnalyzerDefinitionSourceService.class ) ) {
			return serviceRef.get().getLuceneAnalyzerDefinitionProvider();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public LuceneAnalyzerReference createDefaultAnalyzerReference() {
		Class<? extends Analyzer> analyzerClass;
		String analyzerClassName = cfg.getProperty( Environment.ANALYZER_CLASS );
		if ( analyzerClassName != null ) {
			try {
				analyzerClass = ClassLoaderHelper.classForName( analyzerClassName, serviceManager );
			}
			catch (Exception e) {
				// Maybe the string refers to an analyzer definition instead?
				return createNamedAnalyzerReference( analyzerClassName );
			}
		}
		else {
			analyzerClass = StandardAnalyzer.class;
		}
		return createLuceneClassAnalyzerReference( analyzerClass );
	}

	@Override
	public LuceneAnalyzerReference createPassThroughAnalyzerReference() {
		return new SimpleLuceneAnalyzerReference( PassThroughAnalyzer.INSTANCE );
	}

	@Override
	public Map<String, AnalyzerReference> createProvidedAnalyzerReferences() {
		Map<String, AnalyzerReference> references = new HashMap<>();
		for ( String defaultAnalyzerName : defaultAnalyzerDefinitions.keySet() ) {
			NamedLuceneAnalyzerReference reference = createNamedAnalyzerReference( defaultAnalyzerName );
			references.put( defaultAnalyzerName, reference );
		}
		return references;
	}

	@Override
	public LuceneAnalyzerReference createLuceneClassAnalyzerReference(Class<?> analyzerClass) {
		try {
			Analyzer analyzer = ClassLoaderHelper.analyzerInstanceFromClass( analyzerClass, luceneMatchVersion );
			return new SimpleLuceneAnalyzerReference( analyzer );
		}
		catch (ClassCastException e) {
			throw new SearchException( "Lucene analyzer does not extend " + Analyzer.class.getName() + ": " + analyzerClass.getName(), e );
		}
		catch (Exception e) {
			throw new SearchException( "Failed to instantiate lucene analyzer with type " + analyzerClass.getName(), e );
		}
	}

	@Override
	public NamedLuceneAnalyzerReference createNamedAnalyzerReference(String name) {
		return new NamedLuceneAnalyzerReference( name );
	}

	@Override
	public void initializeAnalyzerReferences(Collection<AnalyzerReference> references,
			Map<String, AnalyzerDef> mappingAnalyzerDefinitions) {
		/*
		 * Note that the default definitions may be different
		 * each time a new instance of this strategy is created,
		 * i.e. each time the SearchFactory is "incremented".
		 * Changes to pre-existing default definitions don't matter if the definitions weren't used,
		 * and are harmless if they were already used
		 * (because in that case the reference is already initialized,
		 * so the new version of the definition will be ignored).
		 */
		Map<String, AnalyzerDef> analyzerDefinitions = new HashMap<>( defaultAnalyzerDefinitions );
		analyzerDefinitions.putAll( mappingAnalyzerDefinitions );

		Set<String> existingNamedReferences = new HashSet<>();

		for ( AnalyzerReference reference : references ) {
			if ( reference.is( NamedLuceneAnalyzerReference.class ) ) {
				NamedLuceneAnalyzerReference namedReference = reference.unwrap( NamedLuceneAnalyzerReference.class );
				if ( !namedReference.isInitialized() ) {
					initializeReference( namedReference, analyzerDefinitions );
				}
				existingNamedReferences.add( namedReference.getAnalyzerName() );
			}
			else if ( reference.is( ScopedLuceneAnalyzerReference.class ) ) {
				ScopedLuceneAnalyzerReference scopedReference = reference.unwrap( ScopedLuceneAnalyzerReference.class );
				if ( !scopedReference.isInitialized() ) {
					scopedReference.initialize();
				}
			}
		}
	}

	private void initializeReference(NamedLuceneAnalyzerReference analyzerReference, Map<String, AnalyzerDef> analyzerDefinitions) {
		String name = analyzerReference.getAnalyzerName();

		AnalyzerDef analyzerDefinition = analyzerDefinitions.get( name );
		if ( analyzerDefinition == null ) {
			throw new SearchException( "Lucene analyzer found with an unknown definition: " + name );
		}
		Analyzer analyzer = buildAnalyzer( analyzerDefinition );

		analyzerReference.initialize( analyzer );
	}

	private Analyzer buildAnalyzer(AnalyzerDef analyzerDefinition) {
		try {
			return LuceneAnalyzerBuilder.buildAnalyzer( analyzerDefinition, luceneMatchVersion, serviceManager );
		}
		catch (IOException e) {
			throw new SearchException( "Could not initialize Analyzer definition " + analyzerDefinition, e );
		}
	}

	@Override
	public ScopedLuceneAnalyzerReference.Builder buildScopedAnalyzerReference(AnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedLuceneAnalyzerReference.DeferredInitializationBuilder(
				initialGlobalAnalyzerReference.unwrap( LuceneAnalyzerReference.class ),
				Collections.<String, LuceneAnalyzerReference>emptyMap() );
	}
}
