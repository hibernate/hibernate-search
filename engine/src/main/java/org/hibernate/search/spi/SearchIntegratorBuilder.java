/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.NormalizerDefs;
import org.hibernate.search.backend.impl.BatchedQueueingProcessor;
import org.hibernate.search.backend.impl.QueueingProcessor;
import org.hibernate.search.backend.impl.WorkerFactory;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.impl.DefaultTimingSource;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.impl.ImmutableSearchFactory;
import org.hibernate.search.engine.impl.IncrementalSearchConfiguration;
import org.hibernate.search.engine.impl.MappingDefinitionRegistry;
import org.hibernate.search.engine.impl.MappingModelMetadataProvider;
import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.engine.impl.MutableSearchFactoryState;
import org.hibernate.search.engine.impl.ReflectionReplacingSearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.EntityState;
import org.hibernate.search.engine.spi.SearchMappingHelper;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.filter.impl.CachingWrapperQuery;
import org.hibernate.search.filter.impl.MRUFilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.spi.IndexNameNormalizer;
import org.hibernate.search.spi.impl.ExtendedSearchIntegratorWithShareableState;
import org.hibernate.search.spi.impl.IndexedTypeMaps;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.spi.impl.TypeHierarchy;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Build a search factory following the builder pattern.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class SearchIntegratorBuilder {

	static {
		Version.touch();
	}

	private static final Log log = LoggerFactory.make();

	private SearchConfiguration cfg;
	private MutableSearchFactory rootFactory;
	private final List<Class<?>> classes = new ArrayList<Class<?>>();

	public SearchIntegratorBuilder configuration(SearchConfiguration configuration) {
		this.cfg = configuration;
		return this;
	}

	public SearchIntegratorBuilder currentSearchIntegrator(SearchIntegrator factory) {
		this.rootFactory = factory.unwrap( MutableSearchFactory.class );
		return this;
	}

	public SearchIntegratorBuilder addClass(Class<?> clazz) {
		classes.add( clazz );
		return this;
	}

	private final MutableSearchFactoryState factoryState = new MutableSearchFactoryState();

	public SearchIntegrator buildSearchIntegrator() {
		ExtendedSearchIntegrator extendedIntegrator;
		if ( rootFactory == null ) {
			if ( classes.size() > 0 ) {
				throw new SearchException( "Cannot add a class if the original SearchFactory is not passed" );
			}
			extendedIntegrator = buildNewSearchFactory();
		}
		else {
			extendedIntegrator = buildIncrementalSearchFactory();
		}
		return extendedIntegrator;
	}

	private ExtendedSearchIntegrator buildIncrementalSearchFactory() {
		removeClassesAlreadyManaged();
		if ( classes.size() == 0 ) {
			return rootFactory;
		}
		factoryState.copyStateFromOldFactory( rootFactory );

		final Properties configurationProperties = factoryState.getConfigurationProperties();
		BuildContext buildContext = new BuildContext();

		IncrementalSearchConfiguration searchConfiguration = new IncrementalSearchConfiguration(
				classes,
				configurationProperties,
				factoryState
		);

		applySearchMappingToMetadata(
				searchConfiguration.getReflectionManager(),
				searchConfiguration.getProgrammaticMapping()
		);

		//FIXME The current initDocumentBuilders
		initDocumentBuilders( searchConfiguration, buildContext, searchConfiguration.getProgrammaticMapping() );
		final IndexedTypeMap<EntityIndexBinding> documentBuildersIndexedEntities = factoryState.getIndexBindings();
		IndexedTypeSet indexedClasses = documentBuildersIndexedEntities.keySet();
		for ( EntityIndexBinding entityIndexBinding : documentBuildersIndexedEntities.values() ) {
			//FIXME improve this algorithm to deal with adding new classes to the class hierarchy.
			//Today it seems only safe when a class outside the hierarchy is incrementally added.
			entityIndexBinding.postInitialize( indexedClasses );
		}

		//not really necessary today
		final IndexedTypeMap<DocumentBuilderContainedEntity> documentBuildersContainedEntities = factoryState.getDocumentBuildersContainedEntities();
		for ( DocumentBuilderContainedEntity builder : documentBuildersContainedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}

		//update backend
		//TODO make sure the old IndexManagers and backends are disposed - not currently a problem as we only support adding entities incrementally
		ExtendedSearchIntegratorWithShareableState factory = new ImmutableSearchFactory( factoryState );
		factoryState.setActiveSearchIntegrator( factory );
		rootFactory.setDelegate( factory );
		return rootFactory;
	}

	private void removeClassesAlreadyManaged() {
		Set<Class<?>> remove = new HashSet<Class<?>>();
		final IndexedTypeMap<DocumentBuilderContainedEntity> containedEntities = rootFactory.getDocumentBuildersContainedEntities();
		final IndexedTypeMap<EntityIndexBinding> indexedEntities = rootFactory.getIndexBindings();
		for ( Class<?> entity : classes ) {
			IndexedTypeIdentifier typeId = PojoIndexedTypeIdentifier.convertFromLegacy( entity );
			if ( indexedEntities.containsKey( typeId ) || containedEntities.containsKey( typeId ) ) {
				remove.add( entity );
			}
		}
		for ( Class<?> entity : remove ) {
			classes.remove( entity );
		}
	}

	private ExtendedSearchIntegrator buildNewSearchFactory() {
		BuildContext buildContext = new BuildContext();

		try {
			createNewFactoryState( cfg, buildContext);
		}
		catch (RuntimeException initializationException) {
			try {
				cleanupFactoryState();
			}
			catch (RuntimeException e) {
				initializationException.addSuppressed( e );
			}
			throw initializationException;
		}

		final ExtendedSearchIntegratorWithShareableState factory = new ImmutableSearchFactory( factoryState );
		try {
			factoryState.setActiveSearchIntegrator( factory );
			rootFactory.setDelegate( factory );
			return rootFactory;
		}
		catch (RuntimeException initializationException) {
			// Let the factory do its own cleanup (it may have allocated resource of its own in its constructor)
			try {
				factory.close();
			}
			catch (RuntimeException e) {
				initializationException.addSuppressed( e );
			}
			throw initializationException;
		}
	}

	private void createNewFactoryState(SearchConfiguration cfg2, BuildContext buildContext) {
		createCleanFactoryState( cfg, buildContext );

		final ReflectionManager reflectionManager = getReflectionManager( cfg );
		if ( reflectionManager != cfg.getReflectionManager() ) {
			cfg = new ReflectionReplacingSearchConfiguration( reflectionManager, cfg );
		}

		final SearchMapping mapping = SearchMappingHelper.extractSearchMapping( cfg );
		applySearchMappingToMetadata( reflectionManager, mapping );

		factoryState.setSearchMapping( mapping ); // might be null if feature is not used

		factoryState.setIndexingMode( defineIndexingMode( cfg ) );//need to be done before the document builds
		initDocumentBuilders( cfg, buildContext, mapping );

		final IndexedTypeMap<EntityIndexBinding> documentBuildersIndexedEntities = factoryState.getIndexBindings();
		final IndexedTypeSet indexedClasses = documentBuildersIndexedEntities.keySet();
		for ( EntityIndexBinding entityIndexBinding : documentBuildersIndexedEntities.values() ) {
			entityIndexBinding.postInitialize( indexedClasses );
		}

		// not really necessary today
		final IndexedTypeMap<DocumentBuilderContainedEntity> documentBuildersContainedEntities = factoryState.getDocumentBuildersContainedEntities();
		for ( DocumentBuilderContainedEntity builder : documentBuildersContainedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}

		QueueingProcessor queueingProcessor = new BatchedQueueingProcessor(
				documentBuildersIndexedEntities,
				cfg.getProperties(),
				buildContext.getAllIndexesManager()
		);
		// build worker and back end components
		factoryState.setWorker( WorkerFactory.createWorker( cfg, buildContext, queueingProcessor ) );
		factoryState.setFilterCachingStrategy( buildFilterCachingStrategy( cfg ) );
		factoryState.setCacheBitResultsSize(
				ConfigurationParseHelper.getIntValue(
						cfg.getProperties(), Environment.CACHE_DOCIDRESULTS_SIZE, CachingWrapperQuery.DEFAULT_SIZE
				)
		);
	}

	private void cleanupFactoryState() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			Worker worker = factoryState.getWorker();
			if ( worker != null ) {
				closer.push( worker::close );
			}

			closer.push( factoryState.getAllIndexesManager()::stop );
			closer.push( factoryState.getTimingSource()::stop );
			closer.push( factoryState.getServiceManager()::releaseAllServices );
			closer.pushAll( SearchIntegration::close, factoryState.getIntegrations().values() );
		}
	}

	/**
	 * We need to apply the programmatically configured mapping toe the reflectionManager
	 * to fake the annotations.
	 *
	 * @param reflectionManager assumed a MetadataProviderInjector, it's state will be changed
	 * @param mapping the SearchMapping to apply
	 */
	private void applySearchMappingToMetadata(ReflectionManager reflectionManager, SearchMapping mapping) {
		if ( mapping != null ) {
			if ( !( reflectionManager instanceof MetadataProviderInjector ) ) {
				throw new SearchException(
						"Programmatic mapping model used but ReflectionManager does not implement "
								+ MetadataProviderInjector.class.getName()
				);
			}
			MetadataProviderInjector injector = (MetadataProviderInjector) reflectionManager;
			MetadataProvider original = injector.getMetadataProvider();
			injector.setMetadataProvider( new MappingModelMetadataProvider( original, mapping ) );
		}
	}

	private FilterCachingStrategy buildFilterCachingStrategy(SearchConfiguration searchConfiguration) {
		FilterCachingStrategy filterCachingStrategy;
		String filterCachingStrategyName = searchConfiguration.getProperties()
				.getProperty( Environment.FILTER_CACHING_STRATEGY );
		if ( StringHelper.isEmpty( filterCachingStrategyName ) || "mru".equalsIgnoreCase( filterCachingStrategyName ) ) {
			filterCachingStrategy = new MRUFilterCachingStrategy();
		}
		else {
			Class<?> filterCachingStrategyClass = searchConfiguration.getClassLoaderService()
					.classForName( filterCachingStrategyName );
			filterCachingStrategy = ClassLoaderHelper.instanceFromClass(
					FilterCachingStrategy.class,
					filterCachingStrategyClass,
					"filterCachingStrategy"
			);
		}
		filterCachingStrategy.initialize( searchConfiguration.getProperties() );
		return filterCachingStrategy;
	}

	private void createCleanFactoryState(SearchConfiguration cfg, BuildContext buildContext) {
		if ( rootFactory == null ) {
			//set the mutable structure of factory state
			rootFactory = new MutableSearchFactory();
			factoryState.setDocumentBuildersIndexedEntities( IndexedTypeMaps.concurrentHashMap() );
			factoryState.setDocumentBuildersContainedEntities( IndexedTypeMaps.concurrentHashMap() );
			factoryState.setConfiguredTypeHierarchy( new TypeHierarchy() );
			factoryState.setIndexedTypeHierarchy( new TypeHierarchy() );
			factoryState.setConfigurationProperties( cfg.getProperties() );
			factoryState.setServiceManager(
					new StandardServiceManager(
							cfg,
							buildContext,
							Environment.DEFAULT_SERVICES_MAP
					)
			);
			factoryState.setAllIndexesManager( new IndexManagerHolder() );
			factoryState.setErrorHandler( ErrorHandlerFactory.createErrorHandler( cfg ) );
			factoryState.setInstanceInitializer( cfg.getInstanceInitializer() );
			factoryState.setTimingSource( new DefaultTimingSource() );
			factoryState.setIndexMetadataComplete( cfg.isIndexMetadataComplete() );
			factoryState.setTransactionManagerExpected( cfg.isTransactionManagerExpected() );
			factoryState.setDeleteByTermEnforced( cfg.isDeleteByTermEnforced() );
			factoryState.setIdProvidedImplicit( cfg.isIdProvidedImplicit() );
			factoryState.setMultitenancyEnabled( cfg.isMultitenancyEnabled() );
		}
	}

	/*
	 * Initialize the document builder
	 * This algorithm seems to be safe for incremental search factories.
	 */
	private void initDocumentBuilders(SearchConfiguration searchConfiguration, BuildContext buildContext, SearchMapping searchMapping) {
		ConfigContext configContext = new ConfigContext( searchConfiguration, buildContext, searchMapping,
				factoryState.getIntegrations() );

		initProgrammaticAnalyzers( configContext, searchConfiguration.getReflectionManager() );
		initProgrammaticNormalizers( configContext, searchConfiguration.getReflectionManager() );
		initProgrammaticallyDefinedFilterDef( configContext, searchConfiguration.getReflectionManager() );
		final TypeHierarchy configuredTypeHierarchy = factoryState.getConfiguredTypeHierarchy();
		final TypeHierarchy indexedTypeHierarchy = factoryState.getIndexedTypeHierarchy();
		final IndexedTypeMap<EntityIndexBinding> documentBuildersIndexedEntities = factoryState.getIndexBindings();
		final IndexedTypeMap<DocumentBuilderContainedEntity> documentBuildersContainedEntities = factoryState.getDocumentBuildersContainedEntities();
		final Set<XClass> optimizationBlackListedTypes = new HashSet<XClass>();
		final Map<XClass, Class<?>> classMappings = initializeClassMappings(
				searchConfiguration,
				searchConfiguration.getReflectionManager()
		);

		//we process the @Indexed classes last, so we first start all IndexManager(s).
		final List<XClass> rootIndexedEntities = new LinkedList<XClass>();
		final org.hibernate.search.engine.metadata.impl.MetadataProvider metadataProvider =
				new AnnotationMetadataProvider( searchConfiguration.getReflectionManager(), configContext );

		for ( Map.Entry<XClass, Class<?>> mapping : classMappings.entrySet() ) {
			final XClass mappedXClass = mapping.getKey();
			final Class<?> mappedClass = mapping.getValue();
			final IndexedTypeIdentifier mappedClassIdentifier = new PojoIndexedTypeIdentifier( mappedClass );

			if ( mappedXClass.isAnnotationPresent( Indexed.class ) ) {
				if ( mappedXClass.isAbstract() ) {
					log.abstractClassesCannotInsertDocuments( mappedXClass.getName() );
					continue;
				}

				rootIndexedEntities.add( mappedXClass );
				configuredTypeHierarchy.addConfiguredClass( mappedClass );
				indexedTypeHierarchy.addConfiguredClass( mappedClass );
			}
			else if ( metadataProvider.containsSearchMetadata( mappedClassIdentifier ) ) {
				// For ContainedIn, we get partial metadata information as we can't build
				// the FieldBridges and the analyzers. This is not a problem as these metadata information
				// are only used to track dependencies.
				TypeMetadata typeMetadata = metadataProvider.getTypeMetadataForContainedIn( mappedClassIdentifier );
				final DocumentBuilderContainedEntity documentBuilder = new DocumentBuilderContainedEntity(
						mappedXClass,
						typeMetadata,
						searchConfiguration.getReflectionManager(),
						optimizationBlackListedTypes,
						searchConfiguration.getInstanceInitializer()
				);
				//TODO enhance that, I don't like to expose EntityState
				if ( documentBuilder.getEntityState() != EntityState.NON_INDEXABLE ) {
					documentBuildersContainedEntities.put( mappedClassIdentifier, documentBuilder );
				}
				configuredTypeHierarchy.addConfiguredClass( mappedClass );
			}
			else {
				configuredTypeHierarchy.addConfiguredClass( mappedClass );
			}
		}

		IndexManagerHolder indexesFactory = factoryState.getAllIndexesManager();

		detectIndexNamesCollisions( indexesFactory.getIndexManagers() );

		// Create all IndexManagers, configure and start them:
		for ( XClass mappedXClass : rootIndexedEntities ) {
			final Class mappedClass = classMappings.get( mappedXClass );
			final IndexedTypeIdentifier mappedClassId = new PojoIndexedTypeIdentifier( mappedClass );
			MutableEntityIndexBinding entityIndexBinding = indexesFactory.buildEntityIndexBinding(
					mappedXClass,
					mappedClassId,
					searchConfiguration,
					buildContext
			);

			// interceptor might use non indexed state
			if ( entityIndexBinding.getEntityIndexingInterceptor() != null ) {
				optimizationBlackListedTypes.add( mappedXClass );
			}

			IndexManagerType indexManagerType = entityIndexBinding.getIndexManagerType();

			// Create all DocumentBuilderIndexedEntity
			// FIXME DocumentBuilderIndexedEntity needs to be built by a helper method receiving Class<T> to infer T properly
			// XClass unfortunately is not (yet) genericized: TODO ?
			TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( mappedClassId, indexManagerType );
			final DocumentBuilderIndexedEntity documentBuilder =
					new DocumentBuilderIndexedEntity(
							mappedXClass,
							typeMetadata,
							configContext,
							searchConfiguration.getReflectionManager(),
							optimizationBlackListedTypes,
							searchConfiguration.getInstanceInitializer()
					);
			entityIndexBinding.setDocumentBuilderIndexedEntity( documentBuilder );

			documentBuildersIndexedEntities.put( mappedClassId, entityIndexBinding );
		}

		detectIndexNamesCollisions( indexesFactory.getIndexManagers() );

		disableBlackListedTypesOptimization(
				classMappings,
				optimizationBlackListedTypes,
				documentBuildersIndexedEntities,
				documentBuildersContainedEntities
		);

		factoryState.addFilterDefinitions( configContext.initFilters() );
		factoryState.addIntegrations( configContext.initIntegrations( indexesFactory ) );
	}

	private void detectIndexNamesCollisions(Collection<IndexManager> indexManagers) {
		Map<String, Set<String>> indexNames = collectIndexNames( indexManagers );
		validateIndexNames( indexNames );
	}

	/*
	 * Returns a map containing the set of index names (as value) that generates the same normalized index name (as key).
	 */
	private Map<String, Set<String>> collectIndexNames(Collection<IndexManager> indexManagers) {
		Map<String, Set<String>> indexNames = new HashMap<>();
		for ( IndexManager indexManager : indexManagers ) {
			if ( indexManager instanceof IndexNameNormalizer ) {
				IndexNameNormalizer normalizer = (IndexNameNormalizer) indexManager;
				if ( !indexNames.containsKey( normalizer.getActualIndexName() ) ) {
					indexNames.put( normalizer.getActualIndexName(), new HashSet<String>( 2 ) );
				}
				indexNames.get( normalizer.getActualIndexName() ).add( indexManager.getIndexName() );
			}
		}
		return indexNames;
	}

	/*
	 * The map contains the set of index names (as value) that generates the same normalized index name (as key).
	 */
	private void validateIndexNames(Map<String, Set<String>> indexNames) {
		StringBuilder builder = new StringBuilder();
		String separator = "";
		for ( Entry<String, Set<String>> entry : indexNames.entrySet() ) {
			if ( entry.getValue().size() > 1 ) {
				builder.append( separator );
				builder.append( "(" );
				builder.append( entry.getValue() );
				builder.append( " --> " );
				builder.append( entry.getKey() );
				builder.append( ")" );
				separator = ", ";
			}
		}

		if ( builder.length() > 0 ) {
			throw log.indexNamesCollisionDetected( builder.toString() );
		}
	}

	/**
	 * @param classMappings
	 * @param optimizationBlackListX
	 * @param documentBuildersIndexedEntities
	 * @param documentBuildersContainedEntities
	 */
	private void disableBlackListedTypesOptimization(Map<XClass, Class<?>> classMappings,
			Set<XClass> optimizationBlackListX,
			IndexedTypeMap<EntityIndexBinding> documentBuildersIndexedEntities,
			IndexedTypeMap<DocumentBuilderContainedEntity> documentBuildersContainedEntities) {
		for ( XClass xClass : optimizationBlackListX ) {
			Class<?> type = classMappings.get( xClass );
			if ( type != null ) {
				final PojoIndexedTypeIdentifier typeIdentifier = new PojoIndexedTypeIdentifier( type );
				final EntityIndexBinding entityIndexBinding = documentBuildersIndexedEntities.get( typeIdentifier );
				if ( entityIndexBinding != null ) {
					log.tracef( "Dirty checking optimizations disabled for class %s", type );
					entityIndexBinding.getDocumentBuilder().forceStateInspectionOptimizationsDisabled();
				}
				DocumentBuilderContainedEntity documentBuilderContainedEntity = documentBuildersContainedEntities.get( typeIdentifier );
				if ( documentBuilderContainedEntity != null ) {
					log.tracef( "Dirty checking optimizations disabled for class %s", type );
					documentBuilderContainedEntity.forceStateInspectionOptimizationsDisabled();
				}
			}
		}
	}

	/**
	 * prepares XClasses from configuration
	 */
	private static Map<XClass, Class<?>> initializeClassMappings(SearchConfiguration cfg, ReflectionManager reflectionManager) {
		Iterator<Class<?>> iter = cfg.getClassMappings();
		Map<XClass, Class<?>> map = new HashMap<XClass, Class<?>>();
		while ( iter.hasNext() ) {
			Class<?> mappedClass = iter.next();
			if ( mappedClass == null ) {
				continue;
			}

			XClass mappedXClass = reflectionManager.toXClass( mappedClass );
			if ( mappedXClass == null ) {
				continue;
			}
			map.put( mappedXClass, mappedClass );
		}
		return map;
	}

	private void initProgrammaticAnalyzers(ConfigContext context, ReflectionManager reflectionManager) {
		final Map<?, ?> defaults = reflectionManager.getDefaults();

		if ( defaults != null ) {
			AnalyzerDef[] defs = (AnalyzerDef[]) defaults.get( AnalyzerDefs.class );
			if ( defs != null ) {
				MappingDefinitionRegistry<AnalyzerDef, ?> registry = context.getAnalyzerDefinitionRegistry();
				for ( AnalyzerDef def : defs ) {
					registry.registerGlobal( def.name(), def );
				}
			}
		}
	}

	private void initProgrammaticNormalizers(ConfigContext context, ReflectionManager reflectionManager) {
		final Map<?, ?> defaults = reflectionManager.getDefaults();

		if ( defaults != null ) {
			NormalizerDef[] defs = (NormalizerDef[]) defaults.get( NormalizerDefs.class );
			if ( defs != null ) {
				MappingDefinitionRegistry<NormalizerDef, ?> registry = context.getNormalizerDefinitionRegistry();
				for ( NormalizerDef def : defs ) {
					registry.registerGlobal( def.name(), def );
				}
			}
		}
	}

	private void initProgrammaticallyDefinedFilterDef(ConfigContext context, ReflectionManager reflectionManager) {
		Map<?, ?> defaults = reflectionManager.getDefaults();
		FullTextFilterDef[] filterDefs = (FullTextFilterDef[]) defaults.get( FullTextFilterDefs.class );
		if ( filterDefs != null && filterDefs.length != 0 ) {
			MappingDefinitionRegistry<FullTextFilterDef, ?> registry = context.getFullTextFilterDefinitionRegistry();
			final Map<String, FilterDef> filterDefinitions = factoryState.getFilterDefinitions();
			for ( FullTextFilterDef defAnn : filterDefs ) {
				String name = defAnn.name();
				if ( filterDefinitions.containsKey( name ) ) {
					throw new SearchException( "Multiple definition of @FullTextFilterDef.name=" + defAnn.name() );
				}
				registry.registerGlobal( name, defAnn );
			}
		}
	}

	private ReflectionManager getReflectionManager(SearchConfiguration cfg) {
		ReflectionManager reflectionManager = cfg.getReflectionManager();
		return getReflectionManager( reflectionManager );
	}

	private ReflectionManager getReflectionManager(ReflectionManager reflectionManager) {
		if ( reflectionManager == null ) {
			reflectionManager = new JavaReflectionManager();
		}
		return reflectionManager;
	}

	private static IndexingMode defineIndexingMode(SearchConfiguration cfg) {
		String indexingStrategy = cfg.getProperties().getProperty( Environment.INDEXING_STRATEGY, IndexingMode.EVENT.toExternalRepresentation() );
		return IndexingMode.fromExternalRepresentation( indexingStrategy );
	}

	/**
	 * Implementation of the Hibernate Search SPI WritableBuildContext and WorkerBuildContext
	 * The data is provided by the SearchFactoryState object associated to SearchFactoryBuilder.
	 */
	private class BuildContext implements WorkerBuildContext {
		private final SearchFactoryState factoryState = SearchIntegratorBuilder.this.factoryState;

		@Override
		public ExtendedSearchIntegrator getUninitializedSearchIntegrator() {
			return rootFactory;
		}

		@Override
		public IndexingMode getIndexingMode() {
			return factoryState.getIndexingMode();
		}

		@Override
		public boolean isTransactionManagerExpected() {
			return cfg.isTransactionManagerExpected();
		}

		@Override
		public IndexManagerHolder getAllIndexesManager() {
			return factoryState.getAllIndexesManager();
		}

		@Override
		public ErrorHandler getErrorHandler() {
			return factoryState.getErrorHandler();
		}

		@Override
		public InstanceInitializer getInstanceInitializer() {
			return factoryState.getInstanceInitializer();
		}

		@Override
		public boolean enlistWorkerInTransaction() {
			return factoryState.enlistWorkerInTransaction();
		}

		@Override
		public boolean isIndexMetadataComplete() {
			return factoryState.isIndexMetadataComplete();
		}

		@Override
		public boolean isDeleteByTermEnforced() {
			return factoryState.isDeleteByTermEnforced();
		}

		@Override
		public ServiceManager getServiceManager() {
			return factoryState.getServiceManager();
		}

		@Override
		public boolean isMultitenancyEnabled() {
			return factoryState.isMultitenancyEnabled();
		}

	}
}
