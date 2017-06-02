/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.cfg.EntityDescriptor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * Provides access to some default configuration settings (eg is JPA present, what is the default null token)
 * and holds mapping-scoped configuration (such as analyzer definitions).
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ConfigContext {

	private static final Log LOG = LoggerFactory.make();

	/**
	 * The default token for indexing null values. See {@link org.hibernate.search.annotations.Field#indexNullAs()}
	 */
	private static final String DEFAULT_NULL_INDEX_TOKEN = "_null_";

	private final MappingDefinitionRegistry<AnalyzerDef, AnalyzerDef> analyzerDefinitionRegistry =
			new MappingDefinitionRegistry<>( Function.identity(), LOG::analyzerDefinitionNamingConflict );

	private final MappingDefinitionRegistry<NormalizerDef, NormalizerDef> normalizerDefinitionRegistry =
			new MappingDefinitionRegistry<>( this::interpretNormalizerDef, LOG::normalizerDefinitionNamingConflict );

	private final MappingDefinitionRegistry<FullTextFilterDef, FilterDef> fullTextFilterDefinitionRegistry =
			new MappingDefinitionRegistry<>( this::interpretFullTextFilterDef, LOG::fullTextFilterDefinitionNamingConflict );

	private final Map<IndexManagerType, SearchIntegrationConfigContext> indexManagerTypeConfigContexts = new HashMap<>();

	private final boolean jpaPresent;
	private final String nullToken;
	private final boolean implicitProvidedId;
	private final SearchMapping searchMapping;
	private final ServiceManager serviceManager;
	private final SearchConfiguration searchConfiguration;

	public ConfigContext(SearchConfiguration searchConfiguration, BuildContext buildContext) {
		this( searchConfiguration, buildContext, null, null );
	}

	public ConfigContext(SearchConfiguration searchConfiguration, BuildContext buildContext, SearchMapping searchMapping,
			Map<IndexManagerType, SearchIntegration> previousSearchIntegrations) {
		this.serviceManager = buildContext.getServiceManager();
		this.jpaPresent = searchConfiguration.isJPAAnnotationsProcessingEnabled();
		this.nullToken = initNullToken( searchConfiguration );
		this.implicitProvidedId = searchConfiguration.isIdProvidedImplicit();
		this.searchMapping = searchMapping;
		this.searchConfiguration = searchConfiguration;
		if ( previousSearchIntegrations != null ) {
			for ( Map.Entry<IndexManagerType, SearchIntegration> entry : previousSearchIntegrations.entrySet() ) {
				IndexManagerType type = entry.getKey();
				SearchIntegration integrationState = entry.getValue();
				SearchIntegrationConfigContext context = new SearchIntegrationConfigContext(
						type, serviceManager, searchConfiguration, integrationState );
				indexManagerTypeConfigContexts.put( type, context );
			}
		}
	}

	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	public MappingDefinitionRegistry<AnalyzerDef, ?> getAnalyzerDefinitionRegistry() {
		return analyzerDefinitionRegistry;
	}

	public MappingDefinitionRegistry<NormalizerDef, ?> getNormalizerDefinitionRegistry() {
		return normalizerDefinitionRegistry;
	}

	public MappingDefinitionRegistry<FullTextFilterDef, ?> getFullTextFilterDefinitionRegistry() {
		return fullTextFilterDefinitionRegistry;
	}

	public SearchIntegrationConfigContext forType(IndexManagerType type) {
		SearchIntegrationConfigContext context = indexManagerTypeConfigContexts.get( type );
		if ( context == null ) {
			context = new SearchIntegrationConfigContext( type, serviceManager, searchConfiguration );
			indexManagerTypeConfigContexts.put( type, context );
		}
		return context;
	}

	private String initNullToken(SearchConfiguration cfg) {
		String defaultNullIndexToken = cfg.getProperty( Environment.DEFAULT_NULL_TOKEN );
		if ( StringHelper.isEmpty( defaultNullIndexToken ) ) {
			defaultNullIndexToken = DEFAULT_NULL_INDEX_TOKEN;
		}
		return defaultNullIndexToken;
	}

	public String getDefaultNullToken() {
		return nullToken;
	}

	/**
	 * Check that a given {@link NormalizerDef} is "well-formed",
	 * i.e. that it defines at least one char filter or token filter.
	 *
	 * @param normalizerDef The normalizer def to check
	 * @return The same normalizer def
	 */
	private NormalizerDef interpretNormalizerDef(NormalizerDef normalizerDef) {
		if ( normalizerDef.charFilters().length == 0 && normalizerDef.filters().length == 0 ) {
			throw LOG.invalidEmptyNormalizerDefinition( normalizerDef.name() );
		}

		return normalizerDef;
	}

	private FilterDef interpretFullTextFilterDef(FullTextFilterDef defAnn) {
		FilterDef filterDef = new FilterDef( defAnn );
		if ( filterDef.getImpl().equals( ShardSensitiveOnlyFilter.class ) ) {
			//this is a placeholder don't process regularly
			return filterDef;
		}
		try {
			filterDef.getImpl().newInstance();
		}
		catch (IllegalAccessException e) {
			throw new SearchException( "Unable to create Filter class: " + filterDef.getImpl().getName(), e );
		}
		catch (InstantiationException e) {
			throw new SearchException( "Unable to create Filter class: " + filterDef.getImpl().getName(), e );
		}
		for ( Method method : filterDef.getImpl().getMethods() ) {
			if ( method.isAnnotationPresent( Factory.class ) ) {
				if ( filterDef.getFactoryMethod() != null ) {
					throw new SearchException(
							"Multiple @Factory methods found" + defAnn.name() + ": "
									+ filterDef.getImpl().getName() + "." + method.getName()
					);
				}
				ReflectionHelper.setAccessible( method );
				filterDef.setFactoryMethod( method );
			}
			if ( method.isAnnotationPresent( Key.class ) ) {
				if ( filterDef.getKeyMethod() != null ) {
					throw new SearchException(
							"Multiple @Key methods found" + defAnn.name() + ": "
									+ filterDef.getImpl().getName() + "." + method.getName()
					);
				}
				ReflectionHelper.setAccessible( method );
				filterDef.setKeyMethod( method );
			}

			String name = method.getName();
			if ( name.startsWith( "set" ) && method.getParameterTypes().length == 1 ) {
				filterDef.addSetter( Introspector.decapitalize( name.substring( 3 ) ), method );
			}
		}
		return filterDef;
	}

	/**
	 * Initialize integrations for all discovered index manager types.
	 *
	 * In particular, initialize the named analyzer references created throughout the mapping creation
	 * with the analyzer definitions collected throughout the mapping creation.
	 * <p>
	 * Analyzer definitions and references are handled simultaneously during the mapping
	 * creation, so it's actually possible that, while creating the mapping, we encounter
	 * references to analyzer which haven't been defined yet.
	 * <p>
	 * To work around this issue, we do not resolve references immediately, but instead
	 * create "dangling" references whose initialization will be delayed to the end of
	 * the mapping (see {@link SearchIntegrationConfigContext#getAnalyzerRegistry()}).
	 * <p>
	 * This method executes the final initialization, resolving dangling references.
	 *
	 * @param indexesFactory The index manager holder, giving access to the relevant index manager types.
	 * @return The initialized (and immutable) search integrations.
	 */
	public Map<IndexManagerType, SearchIntegration> initIntegrations(IndexManagerHolder indexesFactory) {
		Map<String, AnalyzerDef> mappingAnalyzerDefs = analyzerDefinitionRegistry.getAll();
		Map<String, NormalizerDef> mappingNormalizerDefs = normalizerDefinitionRegistry.getAll();

		/*
		 * For analyzers/normalizers defined in the mapping, but not referenced in this mapping,
		 * we assume these are Lucene analyzer definitions that will be used
		 * when querying.
		 * Thus we create references to these definitions, so that the references
		 * are initialized below, making the analyzers available at runtime.
		 */
		for ( String name : mappingAnalyzerDefs.keySet() ) {
			if ( !hasAnalyzerReference( name ) ) {
				MutableAnalyzerRegistry registry = forType( LuceneEmbeddedIndexManagerType.INSTANCE )
						.getAnalyzerRegistry();
				registry.getOrCreateAnalyzerReference( name );
			}
		}
		for ( String name : mappingNormalizerDefs.keySet() ) {
			if ( !hasNormalizerReference( name ) ) {
				MutableNormalizerRegistry registry = forType( LuceneEmbeddedIndexManagerType.INSTANCE )
						.getNormalizerRegistry();
				registry.getOrCreateNamedNormalizerReference( name );
			}
		}

		// Put the types in a Set to avoid duplicates
		Set<IndexManagerType> indexManagerTypes = new HashSet<>();
		indexManagerTypes.addAll( indexesFactory.getIndexManagerTypes() );
		// Make sure to initialize every registry, even those that are not used by index managers (see the loop above)
		indexManagerTypes.addAll( indexManagerTypeConfigContexts.keySet() );

		final Map<IndexManagerType, SearchIntegration> immutableSearchIntegrations = new HashMap<>( indexManagerTypes.size() );

		for ( IndexManagerType indexManagerType : indexManagerTypes ) {
			ImmutableSearchIntegration searchIntegration = forType( indexManagerType )
					.initialize( mappingAnalyzerDefs, mappingNormalizerDefs );
			immutableSearchIntegrations.put( indexManagerType, searchIntegration );
		}

		return immutableSearchIntegrations;
	}

	private boolean hasAnalyzerReference(String name) {
		for ( SearchIntegrationConfigContext context : indexManagerTypeConfigContexts.values() ) {
			MutableAnalyzerRegistry registry = context.getAnalyzerRegistry();
			if ( registry.getNamedAnalyzerReferences().containsKey( name ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNormalizerReference(String name) {
		for ( SearchIntegrationConfigContext context : indexManagerTypeConfigContexts.values() ) {
			MutableNormalizerRegistry registry = context.getNormalizerRegistry();
			if ( registry.getNamedNormalizerReferences().containsKey( name ) ) {
				return true;
			}
		}
		return false;
	}

	public Map<String, FilterDef> initFilters() {
		return fullTextFilterDefinitionRegistry.getAll();
	}

	public boolean isJpaPresent() {
		return jpaPresent;
	}

	/**
	 * @return true if we have to assume entities are annotated with @ProvidedId implicitly
	 */
	public boolean isProvidedIdImplicit() {
		return implicitProvidedId;
	}

	/**
	 * Returns class bridge instances configured via the programmatic API, if any. The returned map's values are
	 * {@code @ClassBridge} annotations representing the corresponding analyzer etc. configuration.
	 *
	 * @param type the type for which to return the configured class bridge instances
	 *
	 * @return a map with class bridge instances and their configuration; May be empty but never {@code null}
	 */
	public Map<FieldBridge, ClassBridge> getClassBridgeInstances(Class<?> type) {
		Map<FieldBridge, ClassBridge> classBridgeInstances = null;

		if ( searchMapping != null ) {
			EntityDescriptor entityDescriptor = searchMapping.getEntityDescriptor( type );
			if ( entityDescriptor != null ) {
				classBridgeInstances = entityDescriptor.getClassBridgeConfigurations();
			}
		}

		return classBridgeInstances != null ? classBridgeInstances : Collections.<FieldBridge, ClassBridge>emptyMap();
	}
}
