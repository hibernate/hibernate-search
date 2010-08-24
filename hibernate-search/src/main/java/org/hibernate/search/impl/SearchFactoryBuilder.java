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
package org.hibernate.search.impl;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Similarity;
import org.slf4j.Logger;

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
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.UpdatableBackendQueueProcessorFactory;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.backend.WorkerFactory;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.EntityState;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.filter.CachingWrapperFilter;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.filter.MRUFilterCachingStrategy;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.jmx.IndexControl;
import org.hibernate.search.jmx.JMXRegistrar;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.reader.ReaderProviderFactory;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.WritableBuildContext;
import org.hibernate.search.spi.internals.DirectoryProviderData;
import org.hibernate.search.spi.internals.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.internals.StateSearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.DirectoryProviderFactory;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.PluginLoader;
import org.hibernate.search.util.ReflectionHelper;

/**
 * Build a search factory.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class SearchFactoryBuilder {

	static {
		Version.touch();
	}

	private static final Logger log = LoggerFactory.make();
	private SearchConfiguration cfg;
	private MutableSearchFactory rootFactory;
	private final List<Class<?>> classes = new ArrayList<Class<?>>();

	public SearchFactoryBuilder configuration(SearchConfiguration configuration) {
		this.cfg = configuration;
		return this;
	}

	public SearchFactoryBuilder rootFactory(MutableSearchFactory factory) {
		this.rootFactory = factory;
		return this;
	}

	public SearchFactoryBuilder addClass(Class<?> clazz) {
		classes.add( clazz );
		return this;
	}

	//processing properties
	ReflectionManager reflectionManager;
	String indexingStrategy;
	Map<Class<?>, DocumentBuilderIndexedEntity<?>> documentBuildersIndexedEntities;
	Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities;
	//keep track of the index modifiers per DirectoryProvider since multiple entity can use the same directory provider
	Map<DirectoryProvider<?>, DirectoryProviderData> dirProviderData;
	Worker worker;
	ReaderProvider readerProvider;
	BackendQueueProcessorFactory backendQueueProcessorFactory;
	Map<String, FilterDef> filterDefinitions;
	FilterCachingStrategy filterCachingStrategy;
	Map<String, Analyzer> analyzers;
	int cacheBitResultsSize;
	Properties configurationProperties;
	ErrorHandler errorHandler;
	PolymorphicIndexHierarchy indexHierarchy;
	Map<DirectoryProvider, LuceneIndexingParameters> dirProviderIndexingParams;

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

		String enableJMX = configurationProperties.getProperty( Environment.JMX_ENABLED );
		if ( "true".equalsIgnoreCase( enableJMX ) ) {
			enableIndexControlBean( searchFactoryImplementor );
		}
		return searchFactoryImplementor;
	}

	private void enableIndexControlBean(SearchFactoryImplementor searchFactoryImplementor) {
		if ( !searchFactoryImplementor.isJMXEnabled() ) {
			return;
		}

		// if we don't have a JNDI bound SessionFactory we cannot enable the index control bean
		if ( StringHelper.isEmpty( configurationProperties.getProperty( "hibernate.session_factory_name" ) ) ) {
			log.debug(
					"In order to bind the IndexControlMBean the Hibernate SessionFactory has to be available via JNDI"
			);
			return;
		}

		// since the SearchFactory is mutable we might have an already existing MBean which we have to unregister first
		if ( JMXRegistrar.isNameRegistered( IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME ) ) {
			JMXRegistrar.unRegisterMBean( IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME );
		}

		IndexControl indexCtrlBean = new IndexControl( configurationProperties );
		JMXRegistrar.registerMBean( indexCtrlBean, IndexControl.INDEX_CTRL_MBEAN_OBJECT_NAME );
	}

	private SearchFactoryImplementor buildIncrementalSearchFactory() {
		removeClassesAlreadyManaged();
		if ( classes.size() == 0 ) {
			return rootFactory;
		}

		BuildContext buildContext = new BuildContext();
		copyStateFromOldFactory( rootFactory );
		//TODO we don't keep the reflectionManager. Is that an issue?
		IncrementalSearchConfiguration cfg = new IncrementalSearchConfiguration( classes, configurationProperties );
		reflectionManager = getReflectionManager( cfg );
		//TODO programmatic mapping support

		//FIXME The current initDocumentBuilders
		initDocumentBuilders( cfg, reflectionManager, buildContext );

		Set<Class<?>> indexedClasses = documentBuildersIndexedEntities.keySet();
		for ( DocumentBuilderIndexedEntity builder : documentBuildersIndexedEntities.values() ) {
			//FIXME improve this algorithm to deal with adding new classes to the class hierarchy.
			//Today it seems only safe when a class outside the hierarchy is incrementally added.
			builder.postInitialize( indexedClasses );
		}
		//not really necessary today
		for ( DocumentBuilderContainedEntity builder : documentBuildersContainedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}
		fillSimilarityMapping();

		//update backend
		final BackendQueueProcessorFactory backend = this.backendQueueProcessorFactory;
		if ( backend instanceof UpdatableBackendQueueProcessorFactory ) {
			final UpdatableBackendQueueProcessorFactory updatableBackend = ( UpdatableBackendQueueProcessorFactory ) backend;
			updatableBackend.updateDirectoryProviders( this.dirProviderData.keySet(), buildContext );
		}
		//safe for incremental init at least the ShredBufferReaderProvider
		//this.readerProvider = ReaderProviderFactory.createReaderProvider( cfg, this );
		StateSearchFactoryImplementor factory = new ImmutableSearchFactory( this );
		rootFactory.setDelegate( factory );
		return rootFactory;


	}

	private void removeClassesAlreadyManaged() {
		Set<Class<?>> remove = new HashSet<Class<?>>();
		final Map<Class<?>, DocumentBuilderContainedEntity<?>> containedEntities = rootFactory.getDocumentBuildersContainedEntities();
		final Map<Class<?>, DocumentBuilderIndexedEntity<?>> indexedEntities = rootFactory.getDocumentBuildersIndexedEntities();
		for ( Class<?> entity : classes ) {
			if ( indexedEntities.containsKey( entity ) || containedEntities.containsKey( entity ) ) {
				remove.add( entity );
			}
		}
		for ( Class<?> entity : remove ) {
			classes.remove( entity );
		}
	}

	private void copyStateFromOldFactory(StateSearchFactoryImplementor stateFactory) {
		indexingStrategy = stateFactory.getIndexingStrategy();
		documentBuildersIndexedEntities = stateFactory.getDocumentBuildersIndexedEntities();
		documentBuildersContainedEntities = stateFactory.getDocumentBuildersContainedEntities();
		dirProviderData = stateFactory.getDirectoryProviderData();
		worker = stateFactory.getWorker();
		readerProvider = stateFactory.getReaderProvider();
		backendQueueProcessorFactory = stateFactory.getBackendQueueProcessorFactory();
		filterDefinitions = stateFactory.getFilterDefinitions();
		filterCachingStrategy = stateFactory.getFilterCachingStrategy();
		analyzers = stateFactory.getAnalyzers();
		cacheBitResultsSize = stateFactory.getCacheBitResultsSize();
		configurationProperties = stateFactory.getConfigurationProperties();
		errorHandler = stateFactory.getErrorHandler();
		indexHierarchy = stateFactory.getIndexHierarchy();
		dirProviderIndexingParams = stateFactory.getDirectoryProviderIndexingParams();
	}

	private SearchFactoryImplementor buildNewSearchFactory() {
		createCleanFactoryState();

		configurationProperties = cfg.getProperties();
		errorHandler = createErrorHandler( configurationProperties );
		reflectionManager = getReflectionManager( cfg );
		BuildContext buildContext = new BuildContext();

		final SearchMapping mapping = SearchMappingBuilder.getSearchMapping( cfg );
		if ( mapping != null ) {
			if ( !( reflectionManager instanceof MetadataProviderInjector ) ) {
				throw new SearchException(
						"Programmatic mapping model used but ReflectionManager does not implement "
								+ MetadataProviderInjector.class.getName()
				);
			}
			MetadataProviderInjector injector = ( MetadataProviderInjector ) reflectionManager;
			MetadataProvider original = injector.getMetadataProvider();
			injector.setMetadataProvider( new MappingModelMetadataProvider( original, mapping ) );
		}

		indexingStrategy = defineIndexingStrategy( cfg );//need to be done before the document builds
		dirProviderIndexingParams = new HashMap<DirectoryProvider, LuceneIndexingParameters>();
		initDocumentBuilders( cfg, reflectionManager, buildContext );

		Set<Class<?>> indexedClasses = documentBuildersIndexedEntities.keySet();
		for ( DocumentBuilderIndexedEntity builder : documentBuildersIndexedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}
		//not really necessary today
		for ( DocumentBuilderContainedEntity builder : documentBuildersContainedEntities.values() ) {
			builder.postInitialize( indexedClasses );
		}
		fillSimilarityMapping();

		//build back end
		this.worker = WorkerFactory.createWorker( cfg, buildContext );
		this.readerProvider = ReaderProviderFactory.createReaderProvider( cfg, buildContext );
		this.filterCachingStrategy = buildFilterCachingStrategy( cfg.getProperties() );
		this.cacheBitResultsSize = ConfigurationParseHelper.getIntValue(
				cfg.getProperties(), Environment.CACHE_DOCIDRESULTS_SIZE, CachingWrapperFilter.DEFAULT_SIZE
		);
		StateSearchFactoryImplementor factory = new ImmutableSearchFactory( this );
		rootFactory.setDelegate( factory );
		return rootFactory;
	}

	private void fillSimilarityMapping() {
		for ( DirectoryProviderData directoryConfiguration : dirProviderData.values() ) {
			for ( Class<?> indexedType : directoryConfiguration.getClasses() ) {
				DocumentBuilderIndexedEntity<?> documentBuilder = documentBuildersIndexedEntities.get( indexedType );
				Similarity similarity = documentBuilder.getSimilarity();
				Similarity prevSimilarity = directoryConfiguration.getSimilarity();
				if ( prevSimilarity != null && !prevSimilarity.getClass().equals( similarity.getClass() ) ) {
					throw new SearchException(
							"Multiple entities are sharing the same index but are declaring an " +
									"inconsistent Similarity. When overrriding default Similarity make sure that all types sharing a same index " +
									"declare the same Similarity implementation."
					);
				}
				else {
					directoryConfiguration.setSimilarity( similarity );
				}
			}
		}
	}

	private static FilterCachingStrategy buildFilterCachingStrategy(Properties properties) {
		FilterCachingStrategy filterCachingStrategy;
		String impl = properties.getProperty( Environment.FILTER_CACHING_STRATEGY );
		if ( StringHelper.isEmpty( impl ) || "mru".equalsIgnoreCase( impl ) ) {
			filterCachingStrategy = new MRUFilterCachingStrategy();
		}
		else {
			filterCachingStrategy = PluginLoader.instanceFromName(
					FilterCachingStrategy.class,
					impl, ImmutableSearchFactory.class, "filterCachingStrategy"
			);
		}
		filterCachingStrategy.initialize( properties );
		return filterCachingStrategy;
	}

	private void createCleanFactoryState() {
		if ( rootFactory == null ) {
			rootFactory = new MutableSearchFactory();
			documentBuildersIndexedEntities = new HashMap<Class<?>, DocumentBuilderIndexedEntity<?>>();
			documentBuildersContainedEntities = new HashMap<Class<?>, DocumentBuilderContainedEntity<?>>();
			dirProviderData = new HashMap<DirectoryProvider<?>, DirectoryProviderData>();
			filterDefinitions = new HashMap<String, FilterDef>();
			indexHierarchy = new PolymorphicIndexHierarchy();
		}
	}

	/*
	 * Initialize the document builder
	 * This algorithm seems to be safe for incremental search factories.
	 */

	private void initDocumentBuilders(SearchConfiguration cfg, ReflectionManager reflectionManager, BuildContext buildContext) {
		ConfigContext context = new ConfigContext( cfg );
		Iterator<Class<?>> iter = cfg.getClassMappings();
		DirectoryProviderFactory factory = new DirectoryProviderFactory();

		initProgrammaticAnalyzers( context, reflectionManager );
		initProgrammaticallyDefinedFilterDef( reflectionManager );

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

			if ( mappedXClass.isAnnotationPresent( Indexed.class ) ) {

				if ( mappedXClass.isAbstract() ) {
					log.warn( "Abstract classes can never insert index documents. Remove @Indexed." );
					continue;
				}

				DirectoryProviderFactory.DirectoryProviders providers = factory.createDirectoryProviders(
						mappedXClass, cfg, buildContext, reflectionManager
				);
				//FIXME DocumentBuilderIndexedEntity needs to be built by a helper method receiving Class<T> to infer T properly
				//XClass unfortunately is not (yet) genericized: TODO?
				final DocumentBuilderIndexedEntity<?> documentBuilder = new DocumentBuilderIndexedEntity(
						mappedXClass, context, providers.getProviders(), providers.getSelectionStrategy(),
						reflectionManager
				);

				indexHierarchy.addIndexedClass( mappedClass );
				documentBuildersIndexedEntities.put( mappedClass, documentBuilder );
			}
			else {
				//FIXME DocumentBuilderIndexedEntity needs to be built by a helper method receiving Class<T> to infer T properly
				//XClass unfortunately is not (yet) genericized: TODO?
				final DocumentBuilderContainedEntity<?> documentBuilder = new DocumentBuilderContainedEntity(
						mappedXClass, context, reflectionManager
				);
				//TODO enhance that, I don't like to expose EntityState
				if ( documentBuilder.getEntityState() != EntityState.NON_INDEXABLE ) {
					documentBuildersContainedEntities.put( mappedClass, documentBuilder );
				}
			}
			bindFilterDefs( mappedXClass );
			//TODO should analyzer def for classes at tyher sqme level???
		}
		analyzers = context.initLazyAnalyzers();
		factory.startDirectoryProviders();
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
		if ( filterDefinitions.containsKey( defAnn.name() ) ) {
			throw new SearchException(
					"Multiple definition of @FullTextFilterDef.name=" + defAnn.name() + ": "
							+ mappedXClass.getName()
			);
		}

		bindFullTextFilterDef( defAnn );
	}

	private void bindFullTextFilterDef(FullTextFilterDef defAnn) {
		FilterDef filterDef = new FilterDef( defAnn );
		if ( filterDef.getImpl().equals( ShardSensitiveOnlyFilter.class ) ) {
			//this is a placeholder don't process regularly
			filterDefinitions.put( defAnn.name(), filterDef );
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
		filterDefinitions.put( defAnn.name(), filterDef );
	}

	private void initProgrammaticAnalyzers(ConfigContext context, ReflectionManager reflectionManager) {
		final Map defaults = reflectionManager.getDefaults();

		if ( defaults != null ) {
			AnalyzerDef[] defs = ( AnalyzerDef[] ) defaults.get( AnalyzerDefs.class );
			if ( defs != null ) {
				for ( AnalyzerDef def : defs ) {
					context.addAnalyzerDef( def );
				}
			}
		}
	}

	private void initProgrammaticallyDefinedFilterDef(ReflectionManager reflectionManager) {
		@SuppressWarnings("unchecked") Map defaults = reflectionManager.getDefaults();
		FullTextFilterDef[] filterDefs = ( FullTextFilterDef[] ) defaults.get( FullTextFilterDefs.class );
		if ( filterDefs != null && filterDefs.length != 0 ) {
			for ( FullTextFilterDef defAnn : filterDefs ) {
				if ( filterDefinitions.containsKey( defAnn.name() ) ) {
					throw new SearchException( "Multiple definition of @FullTextFilterDef.name=" + defAnn.name() );
				}
				bindFullTextFilterDef( defAnn );
			}
		}
	}

	private static ErrorHandler createErrorHandler(Properties configuration) {
		String errorHandlerClassName = configuration.getProperty( Environment.ERROR_HANDLER );
		if ( StringHelper.isEmpty( errorHandlerClassName ) ) {
			return new LogErrorHandler();
		}
		else if ( errorHandlerClassName.trim().equals( "log" ) ) {
			return new LogErrorHandler();
		}
		else {
			return PluginLoader.instanceFromName(
					ErrorHandler.class, errorHandlerClassName,
					ImmutableSearchFactory.class, "Error Handler"
			);
		}
	}

	private ReflectionManager getReflectionManager(SearchConfiguration cfg) {
		ReflectionManager reflectionManager = cfg.getReflectionManager();
		return geReflectionManager( reflectionManager );
	}

	private ReflectionManager geReflectionManager(ReflectionManager reflectionManager) {
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

	private class BuildContext implements WritableBuildContext, WorkerBuildContext {
		public void addOptimizerStrategy(DirectoryProvider<?> provider, OptimizerStrategy optimizerStrategy) {
			DirectoryProviderData data = dirProviderData.get( provider );
			if ( data == null ) {
				data = new DirectoryProviderData();
				dirProviderData.put( provider, data );
			}
			data.setOptimizerStrategy( optimizerStrategy );
		}

		public void addIndexingParameters(DirectoryProvider<?> provider, LuceneIndexingParameters indexingParams) {
			dirProviderIndexingParams.put( provider, indexingParams );
		}

		public void addClassToDirectoryProvider(Class<?> entity, DirectoryProvider<?> directoryProvider, boolean exclusiveIndexUsage) {
			DirectoryProviderData data = dirProviderData.get( directoryProvider );
			if ( data == null ) {
				data = new DirectoryProviderData();
				dirProviderData.put( directoryProvider, data );
			}
			data.getClasses().add( entity );
			data.setExclusiveIndexUsage( exclusiveIndexUsage );
		}

		public SearchFactoryImplementor getUninitializedSearchFactory() {
			return rootFactory;
		}

		public String getIndexingStrategy() {
			return indexingStrategy;
		}

		public Set<DirectoryProvider<?>> getDirectoryProviders() {
			return SearchFactoryBuilder.this.dirProviderData.keySet();
		}

		public void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory) {
			SearchFactoryBuilder.this.backendQueueProcessorFactory = backendQueueProcessorFactory;
		}

		public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider) {
			return dirProviderData.get( provider ).getOptimizerStrategy();
		}

		public Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> directoryProvider) {
			return Collections.unmodifiableSet( dirProviderData.get( directoryProvider ).getClasses() );
		}

		public LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider) {
			return dirProviderIndexingParams.get( provider );
		}

		public ReentrantLock getDirectoryProviderLock(DirectoryProvider<?> dp) {
			return SearchFactoryBuilder.this.dirProviderData.get( dp ).getDirLock();
		}

		public Similarity getSimilarity(DirectoryProvider<?> provider) {
			Similarity similarity = dirProviderData.get( provider ).getSimilarity();
			if ( similarity == null ) {
				throw new SearchException( "Assertion error: a similarity should be defined for each provider" );
			}
			return similarity;
		}

		public boolean isExclusiveIndexUsageEnabled(DirectoryProvider<?> provider) {
			return dirProviderData.get( provider ).isExclusiveIndexUsage();
		}

		public ErrorHandler getErrorHandler() {
			return errorHandler;
		}

		@SuppressWarnings("unchecked")
		public <T> DocumentBuilderIndexedEntity<T> getDocumentBuilderIndexedEntity(Class<T> entityType) {
			return ( DocumentBuilderIndexedEntity<T> ) documentBuildersIndexedEntities.get( entityType );
		}

	}
}
