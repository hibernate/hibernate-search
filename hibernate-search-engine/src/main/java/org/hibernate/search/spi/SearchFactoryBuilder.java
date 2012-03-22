/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.spi;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.impl.BatchedQueueingProcessor;
import org.hibernate.search.backend.impl.QueueingProcessor;
import org.hibernate.search.backend.impl.WorkerFactory;
import org.hibernate.search.engine.impl.DefaultTimingSource;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.EntityState;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.filter.impl.CachingWrapperFilter;
import org.hibernate.search.filter.impl.MRUFilterCachingStrategy;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.impl.ImmutableSearchFactory;
import org.hibernate.search.impl.IncrementalSearchConfiguration;
import org.hibernate.search.impl.MappingModelMetadataProvider;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.impl.MutableSearchFactoryState;
import org.hibernate.search.impl.ReflectionReplacingSearchConfiguration;
import org.hibernate.search.impl.SearchMappingBuilder;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.internals.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.internals.SearchFactoryImplementorWithShareableState;
import org.hibernate.search.spi.internals.SearchFactoryState;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Build a search factory following the builder pattern.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class SearchFactoryBuilder {

	static {
		Version.touch();
	}

	private static final Log log = LoggerFactory.make();

	private SearchConfiguration cfg;
	private MutableSearchFactory rootFactory;
	private final List<Class<?>> classes = new ArrayList<Class<?>>();

	public SearchFactoryBuilder configuration(SearchConfiguration configuration) {
		this.cfg = configuration;
		return this;
	}

	public SearchFactoryBuilder currentFactory(SearchFactoryIntegrator factory) {
		//We know that the only expected concrete type is MutableSearchFactory
		//This could fail if some fancy framework proxy the object but at this stage they likely won't
		//even proxy SearchFactoryIntegrator.
		//If that happens we can provide a unwrap method to SearchFactory
		this.rootFactory = (MutableSearchFactory) factory;
		return this;
	}

	public SearchFactoryBuilder addClass(Class<?> clazz) {
		classes.add( clazz );
		return this;
	}

	private final MutableSearchFactoryState factoryState = new MutableSearchFactoryState();

	public SearchFactoryImplementor buildSearchFactory() {
		SearchFactoryImplementor searchFactoryImplementor;
		if ( rootFactory == null ) {
			if ( classes.size() > 0 ) {
				throw new SearchException( "Cannot add a class if the original SearchFactory is not passed" );
			}
			searchFactoryImplementor = buildNewSearchFactory();
		}
		else {
			searchFactoryImplementor = buildIncrementalSearchFactory();
		}
		return searchFactoryImplementor;
	}

	private SearchFactoryImplementor buildIncrementalSearchFactory() {
		removeClassesAlreadyManaged();
		if ( classes.size() == 0 ) {
			return rootFactory;
		}
		factoryState.copyStateFromOldFactory( rootFactory );

		final Properties configurationProperties = factoryState.getConfigurationProperties();
		BuildContext buildContext = new BuildContext();
		
		IncrementalSearchConfiguration cfg = new IncrementalSearchConfiguration( classes, configurationProperties, factoryState );

		applySearchMappingToMetadata( cfg.getReflectionManager(), cfg.getProgrammaticMapping() );

		//FIXME The current initDocumentBuilders
		initDocumentBuilders( cfg, buildContext );
		final Map<Class<?>, EntityIndexBinder> documentBuildersIndexedEntities = factoryState.getIndexBindingForEntity();
		Set<Class<?>> indexedClasses = documentBuildersIndexedEntities.keySet();
		for ( EntityIndexBinder builder : documentBuildersIndexedEntities.values() ) {
			//FIXME improve this algorithm to deal with adding new classes to the class hierarchy.
			//Today it seems only safe when a class outside the hierarchy is incrementally added.
			builder.postInitialize( indexedClasses );
		}
		//not really necessary today
		final Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities = factoryState.getDocumentBuildersContainedEntities();
		for ( DocumentBuilderContainedEntity builder : documentBuildersContainedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}
		fillSimilarityMapping();

		//update backend
		//TODO make sure the old IndexManagers and backends are disposed - not currently a problem as we only support adding entities incrementally
		SearchFactoryImplementorWithShareableState factory = new ImmutableSearchFactory( factoryState );
		factoryState.setActiveSearchFactory( factory );
		rootFactory.setDelegate( factory );
		return rootFactory;
	}

	private void removeClassesAlreadyManaged() {
		Set<Class<?>> remove = new HashSet<Class<?>>();
		final Map<Class<?>, DocumentBuilderContainedEntity<?>> containedEntities = rootFactory.getDocumentBuildersContainedEntities();
		final Map<Class<?>, EntityIndexBinder> indexedEntities = rootFactory.getIndexBindingForEntity();
		for ( Class<?> entity : classes ) {
			if ( indexedEntities.containsKey( entity ) || containedEntities.containsKey( entity ) ) {
				remove.add( entity );
			}
		}
		for ( Class<?> entity : remove ) {
			classes.remove( entity );
		}
	}

	private SearchFactoryImplementor buildNewSearchFactory() {
		createCleanFactoryState( cfg );

		final ReflectionManager reflectionManager = getReflectionManager( cfg );
		if ( reflectionManager != cfg.getReflectionManager() ) {
			cfg = new ReflectionReplacingSearchConfiguration( reflectionManager, cfg );
		}

		BuildContext buildContext = new BuildContext();

		final SearchMapping mapping = SearchMappingBuilder.getSearchMapping( cfg );
		applySearchMappingToMetadata( reflectionManager, mapping );

		factoryState.setSearchMapping( mapping ); // might be null if feature is not used

		factoryState.setIndexingStrategy( defineIndexingStrategy( cfg ) );//need to be done before the document builds
		initDocumentBuilders( cfg, buildContext );

		final Map<Class<?>, EntityIndexBinder> documentBuildersIndexedEntities = factoryState.getIndexBindingForEntity();
		Set<Class<?>> indexedClasses = documentBuildersIndexedEntities.keySet();
		for ( EntityIndexBinder builder : documentBuildersIndexedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}
		//not really necessary today
		final Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities = factoryState.getDocumentBuildersContainedEntities();
		for ( DocumentBuilderContainedEntity builder : documentBuildersContainedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}
		fillSimilarityMapping();

		QueueingProcessor queueingProcessor = new BatchedQueueingProcessor( documentBuildersIndexedEntities, cfg.getProperties() );
		//build worker and back end components
		factoryState.setWorker( WorkerFactory.createWorker( cfg, buildContext, queueingProcessor) );
		factoryState.setFilterCachingStrategy( buildFilterCachingStrategy( cfg.getProperties() ) );
		factoryState.setCacheBitResultsSize(
				ConfigurationParseHelper.getIntValue(
						cfg.getProperties(), Environment.CACHE_DOCIDRESULTS_SIZE, CachingWrapperFilter.DEFAULT_SIZE
				)
		);
		SearchFactoryImplementorWithShareableState factory = new ImmutableSearchFactory( factoryState );
		factoryState.setActiveSearchFactory( factory );
		rootFactory.setDelegate( factory );
		return rootFactory;
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

	private void fillSimilarityMapping() {
		//TODO cleanup: this logic to select the Similarity is too complex, should likely be done in a previous phase
		final Map<Class<?>, EntityIndexBinder> documentBuildersIndexedEntities = factoryState.getIndexBindingForEntity();
		for ( Entry<Class<?>, EntityIndexBinder> entry : documentBuildersIndexedEntities.entrySet() ) {
			Class<?> clazz = entry.getKey();
			EntityIndexBinder entityMapping = entry.getValue();
			Similarity entitySimilarity = entityMapping.getSimilarity();
			if ( entitySimilarity == null ) {
				//might have been read from annotations, fill the missing information in the EntityIndexBinder:
				entitySimilarity = entityMapping.getDocumentBuilder().getSimilarity();
				if ( entitySimilarity != null ) {
					MutableEntityIndexBinding newMapping = buildTypeSafeMutableEntityBinder(
							clazz,
							entityMapping,
							entitySimilarity
					);
					entityMapping = newMapping;
					documentBuildersIndexedEntities.put( clazz, entityMapping );
				}
			}
			IndexManager[] indexManagers = entityMapping.getIndexManagers();
			for ( IndexManager indexManager : indexManagers ) {
				Similarity indexSimilarity = indexManager.getSimilarity();
				if ( entitySimilarity != null && indexSimilarity == null ) {
					indexManager.setSimilarity( entitySimilarity );
				}
				else if ( entitySimilarity != null && ! entitySimilarity.getClass().equals( indexSimilarity.getClass() ) ) {
					throw new SearchException(
							"Multiple entities are sharing the same index but are declaring an " +
									"inconsistent Similarity. When overriding default Similarity make sure that all types sharing a same index " +
									"declare the same Similarity implementation."
					);
				}
			}
		}
	}

	@SuppressWarnings( "unchecked" )
	private <T> MutableEntityIndexBinding<T> buildTypeSafeMutableEntityBinder(Class<T> clazz, EntityIndexBinder entityMapping, Similarity entitySimilarity) {
		EntityIndexingInterceptor<? super T> interceptor = (EntityIndexingInterceptor<? super T> ) entityMapping.getEntityIndexingInterceptor();
		MutableEntityIndexBinding<T> newMapping = new MutableEntityIndexBinding<T>(
				entityMapping.getSelectionStrategy(),
				entitySimilarity,
				entityMapping.getIndexManagers(),
				interceptor
		);
		DocumentBuilderIndexedEntity<T> documentBuilder = (DocumentBuilderIndexedEntity<T>) entityMapping.getDocumentBuilder();
		newMapping.setDocumentBuilderIndexedEntity( documentBuilder );
		return newMapping;
	}

	private static FilterCachingStrategy buildFilterCachingStrategy(Properties properties) {
		FilterCachingStrategy filterCachingStrategy;
		String impl = properties.getProperty( Environment.FILTER_CACHING_STRATEGY );
		if ( StringHelper.isEmpty( impl ) || "mru".equalsIgnoreCase( impl ) ) {
			filterCachingStrategy = new MRUFilterCachingStrategy();
		}
		else {
			filterCachingStrategy = ClassLoaderHelper.instanceFromName(
					FilterCachingStrategy.class,
					impl, ImmutableSearchFactory.class, "filterCachingStrategy"
			);
		}
		filterCachingStrategy.initialize( properties );
		return filterCachingStrategy;
	}

	private void createCleanFactoryState(SearchConfiguration cfg) {
		if ( rootFactory == null ) {
			//set the mutable structure of factory state
			rootFactory = new MutableSearchFactory();
			factoryState.setDocumentBuildersIndexedEntities( new ConcurrentHashMap<Class<?>, EntityIndexBinder>() );
			factoryState.setDocumentBuildersContainedEntities( new ConcurrentHashMap<Class<?>, DocumentBuilderContainedEntity<?>>() );
			factoryState.setFilterDefinitions( new ConcurrentHashMap<String, FilterDef>() );
			factoryState.setIndexHierarchy( new PolymorphicIndexHierarchy() );
			factoryState.setConfigurationProperties( cfg.getProperties() );
			factoryState.setServiceManager( new ServiceManager( cfg ) );
			factoryState.setAllIndexesManager( new IndexManagerHolder() );
			factoryState.setErrorHandler( createErrorHandler( cfg ) );
			factoryState.setInstanceInitializer( cfg.getInstanceInitializer() );
			factoryState.setTimingSource( new DefaultTimingSource() );
			factoryState.setIndexMetadataComplete( cfg.isIndexMetadataComplete() );
			factoryState.setTransactionManagerExpected( cfg.isTransactionManagerExpected() );
		}
	}

	/*
	 * Initialize the document builder
	 * This algorithm seems to be safe for incremental search factories.
	 */
	private void initDocumentBuilders(SearchConfiguration cfg, BuildContext buildContext) {
		ConfigContext context = new ConfigContext( cfg );

		initProgrammaticAnalyzers( context, cfg.getReflectionManager() );
		initProgrammaticallyDefinedFilterDef( cfg.getReflectionManager() );
		final PolymorphicIndexHierarchy indexingHierarchy = factoryState.getIndexHierarchy();
		final Map<Class<?>, EntityIndexBinder> documentBuildersIndexedEntities = factoryState.getIndexBindingForEntity();
		final Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities = factoryState.getDocumentBuildersContainedEntities();
		final Set<XClass> optimizationBlackListedTypes = new HashSet<XClass>();
		final Map<XClass, Class> classMappings = initializeClassMappings( cfg, cfg.getReflectionManager() );
		
		//we process the @Indexed classes last, so we first start all IndexManager(s).
		final List<XClass> rootIndexedEntities = new LinkedList<XClass>();
		
		for ( Map.Entry<XClass, Class> mapping : classMappings.entrySet() ) {

			XClass mappedXClass = mapping.getKey();
			Class mappedClass = mapping.getValue();
			
			if ( mappedXClass.isAnnotationPresent( Indexed.class ) ) {

				if ( mappedXClass.isAbstract() ) {
					log.abstractClassesCannotInsertDocuments();
					continue;
				}

				rootIndexedEntities.add( mappedXClass );
				indexingHierarchy.addIndexedClass( mappedClass );
			}
			else {
				//FIXME DocumentBuilderIndexedEntity needs to be built by a helper method receiving Class<T> to infer T properly
				//XClass unfortunately is not (yet) genericized: TODO?
				final DocumentBuilderContainedEntity<?> documentBuilder = new DocumentBuilderContainedEntity(
						mappedXClass, context, cfg.getReflectionManager(), optimizationBlackListedTypes, cfg.getInstanceInitializer()
				);
				//TODO enhance that, I don't like to expose EntityState
				if ( documentBuilder.getEntityState() != EntityState.NON_INDEXABLE ) {
					documentBuildersContainedEntities.put( mappedClass, documentBuilder );
				}
			}
			bindFilterDefs( mappedXClass );
			//TODO should analyzer def for classes at their same level???
		}
		
		IndexManagerHolder indexesFactory = factoryState.getAllIndexesManager();
		
		// Create all IndexManagers, configure and start them:
		for ( XClass mappedXClass : rootIndexedEntities ) {
			
			Class mappedClass = classMappings.get( mappedXClass );
			MutableEntityIndexBinding mappedEntity = indexesFactory.buildEntityIndexBinding( mappedXClass, mappedClass, cfg, buildContext );
			//interceptor might use non indexed state
		    if ( mappedEntity.getEntityIndexingInterceptor() != null ) {
				optimizationBlackListedTypes.add( mappedXClass );
			}
			// Create all DocumentBuilderIndexedEntity
			//FIXME DocumentBuilderIndexedEntity needs to be built by a helper method receiving Class<T> to infer T properly
			//XClass unfortunately is not (yet) genericized: TODO?
			final DocumentBuilderIndexedEntity<?> documentBuilder =
					new DocumentBuilderIndexedEntity(
							mappedXClass,
							context,
							mappedEntity.getSimilarity(),
							cfg.getReflectionManager(),
							optimizationBlackListedTypes,
							cfg.getInstanceInitializer()
					);
			mappedEntity.setDocumentBuilderIndexedEntity( documentBuilder );

			documentBuildersIndexedEntities.put( mappedClass, mappedEntity );
		}
		
		disableBlackListedTypesOptimization( classMappings, optimizationBlackListedTypes, documentBuildersIndexedEntities, documentBuildersContainedEntities );
		factoryState.setAnalyzers( context.initLazyAnalyzers() );
	}

	/**
	 * @param classMappings
	 * @param optimizationBlackListX
	 * @param documentBuildersIndexedEntities
	 * @param documentBuildersContainedEntities
	 */
	private void disableBlackListedTypesOptimization(Map<XClass, Class> classMappings,
			Set<XClass> optimizationBlackListX,
			Map<Class<?>, EntityIndexBinder> documentBuildersIndexedEntities,
			Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities) {
		for ( XClass xClass : optimizationBlackListX ) {
			Class type = classMappings.get( xClass );
			if ( type != null ) {
				EntityIndexBinder entityIndexBinding = documentBuildersIndexedEntities.get( type );
				if ( entityIndexBinding != null ) {
					log.tracef( "Dirty checking optimizations disabled for class %s", type );
					entityIndexBinding.getDocumentBuilder().forceStateInspectionOptimizationsDisabled();
				}
				DocumentBuilderContainedEntity<?> documentBuilderContainedEntity = documentBuildersContainedEntities.get( type );
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
	private static Map<XClass, Class> initializeClassMappings(SearchConfiguration cfg, ReflectionManager reflectionManager) {
		Iterator<Class<?>> iter = cfg.getClassMappings();
		Map<XClass, Class> map = new HashMap<XClass, Class>();
		while ( iter.hasNext() ) {
			Class<?> mappedClass = iter.next();
			if ( mappedClass == null ) {
				continue;
			}
			@SuppressWarnings("unchecked")
			XClass mappedXClass = reflectionManager.toXClass( mappedClass );
			if ( mappedXClass == null ) {
				continue;
			}
			map.put( mappedXClass, mappedClass );
		}
		return map;
	}

	private void bindFilterDefs(XClass mappedXClass) {
		FullTextFilterDef defAnn = mappedXClass.getAnnotation( FullTextFilterDef.class );
		if ( defAnn != null ) {
			bindFilterDef( defAnn, mappedXClass );
		}
		FullTextFilterDefs defsAnn = mappedXClass.getAnnotation( FullTextFilterDefs.class );
		if ( defsAnn != null ) {
			for ( FullTextFilterDef def : defsAnn.value() ) {
				bindFilterDef( def, mappedXClass );
			}
		}
	}

	private void bindFilterDef(FullTextFilterDef defAnn, XClass mappedXClass) {
		if ( factoryState.getFilterDefinitions().containsKey( defAnn.name() ) ) {
			throw new SearchException(
					"Multiple definition of @FullTextFilterDef.name=" + defAnn.name() + ": "
							+ mappedXClass.getName()
			);
		}

		bindFullTextFilterDef( defAnn );
	}

	private void bindFullTextFilterDef(FullTextFilterDef defAnn) {
		FilterDef filterDef = new FilterDef( defAnn );
		final Map<String, FilterDef> filterDefinition = factoryState.getFilterDefinitions();
		if ( filterDef.getImpl().equals( ShardSensitiveOnlyFilter.class ) ) {
			//this is a placeholder don't process regularly
			filterDefinition.put( defAnn.name(), filterDef );
			return;
		}
		try {
			filterDef.getImpl().newInstance();
		}
		catch ( IllegalAccessException e ) {
			throw new SearchException( "Unable to create Filter class: " + filterDef.getImpl().getName(), e );
		}
		catch ( InstantiationException e ) {
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
		filterDefinition.put( defAnn.name(), filterDef );
	}

	private void initProgrammaticAnalyzers(ConfigContext context, ReflectionManager reflectionManager) {
		final Map defaults = reflectionManager.getDefaults();

		if ( defaults != null ) {
			AnalyzerDef[] defs = (AnalyzerDef[]) defaults.get( AnalyzerDefs.class );
			if ( defs != null ) {
				for ( AnalyzerDef def : defs ) {
					context.addGlobalAnalyzerDef( def );
				}
			}
		}
	}

	private void initProgrammaticallyDefinedFilterDef(ReflectionManager reflectionManager) {
		@SuppressWarnings("unchecked") Map defaults = reflectionManager.getDefaults();
		FullTextFilterDef[] filterDefs = (FullTextFilterDef[]) defaults.get( FullTextFilterDefs.class );
		if ( filterDefs != null && filterDefs.length != 0 ) {
			final Map<String, FilterDef> filterDefinitions = factoryState.getFilterDefinitions();
			for ( FullTextFilterDef defAnn : filterDefs ) {
				if ( filterDefinitions.containsKey( defAnn.name() ) ) {
					throw new SearchException( "Multiple definition of @FullTextFilterDef.name=" + defAnn.name() );
				}
				bindFullTextFilterDef( defAnn );
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

	private static String defineIndexingStrategy(SearchConfiguration cfg) {
		String indexingStrategy = cfg.getProperties().getProperty( Environment.INDEXING_STRATEGY, "event" );
		if ( !( "event".equals( indexingStrategy ) || "manual".equals( indexingStrategy ) ) ) {
			throw new SearchException( Environment.INDEXING_STRATEGY + " unknown: " + indexingStrategy );
		}
		return indexingStrategy;
	}

	public static ErrorHandler createErrorHandler(SearchConfiguration searchCfg) {
		String errorHandlerClassName = searchCfg.getProperty( Environment.ERROR_HANDLER );
		if ( StringHelper.isEmpty( errorHandlerClassName ) ) {
			return new LogErrorHandler();
		}
		else if ( errorHandlerClassName.trim().equals( "log" ) ) {
			return new LogErrorHandler();
		}
		else {
			return ClassLoaderHelper.instanceFromName(
					ErrorHandler.class, errorHandlerClassName,
					ImmutableSearchFactory.class, "Error Handler"
			);
		}
	}

	/**
	 * Implementation of the Hibernate Search SPI WritableBuildContext and WorkerBuildContext
	 * The data is provided by the SearchFactoryState object associated to SearchFactoryBuilder.
	 */
	private class BuildContext implements WritableBuildContext, WorkerBuildContext {
		private final SearchFactoryState factoryState = SearchFactoryBuilder.this.factoryState;

		public SearchFactoryImplementor getUninitializedSearchFactory() {
			return rootFactory;
		}

		public String getIndexingStrategy() {
			return factoryState.getIndexingStrategy();
		}

		public <T> T requestService(Class<? extends ServiceProvider<T>> provider) {
			return factoryState.getServiceManager().requestService( provider, this );
		}

		public void releaseService(Class<? extends ServiceProvider<?>> provider) {
			factoryState.getServiceManager().releaseService( provider );
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
		public boolean isIndexMetadataComplete() {
			return factoryState.isIndexMetadataComplete();
		}

	}
}
