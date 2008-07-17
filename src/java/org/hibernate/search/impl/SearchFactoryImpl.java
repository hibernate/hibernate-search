//$Id$
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
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.backend.WorkerFactory;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.filter.CachingWrapperFilter;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.filter.MRUFilterCachingStrategy;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.reader.ReaderProviderFactory;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.DirectoryProviderFactory;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class SearchFactoryImpl implements SearchFactoryImplementor {
	private static final ThreadLocal<WeakHashMap<SearchConfiguration, SearchFactoryImpl>> contexts =
			new ThreadLocal<WeakHashMap<SearchConfiguration, SearchFactoryImpl>>();

	static {
		Version.touch();
	}

	private final Logger log = LoggerFactory.getLogger( SearchFactoryImpl.class );

	private final Map<Class, DocumentBuilder<Object>> documentBuilders = new HashMap<Class, DocumentBuilder<Object>>();
	//keep track of the index modifiers per DirectoryProvider since multiple entity can use the same directory provider
	private final Map<DirectoryProvider, DirectoryProviderData> dirProviderData = new HashMap<DirectoryProvider, DirectoryProviderData>();
	private final Worker worker;
	private final ReaderProvider readerProvider;
	private BackendQueueProcessorFactory backendQueueProcessorFactory;
	private final Map<String, FilterDef> filterDefinitions = new HashMap<String, FilterDef>();
	private final FilterCachingStrategy filterCachingStrategy;
	private Map<String, Analyzer> analyzers;
	private final AtomicBoolean stopped = new AtomicBoolean( false );
	private final int cacheBitResultsSize;
	/*
	 * used as a barrier (piggyback usage) between initialization and subsequent usage of searchFactory in different threads
	 * this is due to our use of the initialize pattern is a few areas
	 * subsequent reads on volatiles should be very cheap on most platform especially since we don't write after init
	 *
	 * This volatile is meant to be written after initialization
	 * and read by all subsequent methods accessing the SearchFactory state
	 * read to be as barrier != 0. If barrier == 0 we have a race condition, but is not likely to happen.
	 */
	private volatile short barrier;

	/**
	 * Each directory provider (index) can have its own performance settings.
	 */
	private Map<DirectoryProvider, LuceneIndexingParameters> dirProviderIndexingParams =
		new HashMap<DirectoryProvider, LuceneIndexingParameters>();
	private final String indexingStrategy;


	public BackendQueueProcessorFactory getBackendQueueProcessorFactory() {
		if (barrier != 0) { } //read barrier
		return backendQueueProcessorFactory;
	}

	public void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory) {
		//no need to set a barrier, we init in the same thread as the init one
		this.backendQueueProcessorFactory = backendQueueProcessorFactory;
	}

	public SearchFactoryImpl(SearchConfiguration cfg) {
		ReflectionManager reflectionManager = cfg.getReflectionManager();
		if ( reflectionManager == null ) {
			reflectionManager = new JavaReflectionManager();
		}
		this.indexingStrategy = defineIndexingStrategy( cfg ); //need to be done before the document builds
		initDocumentBuilders( cfg, reflectionManager );

		Set<Class> indexedClasses = documentBuilders.keySet();
		for (DocumentBuilder builder : documentBuilders.values()) {
			builder.postInitialize( indexedClasses );
		}
		this.worker = WorkerFactory.createWorker( cfg, this );
		this.readerProvider = ReaderProviderFactory.createReaderProvider( cfg, this );
		this.filterCachingStrategy = buildFilterCachingStrategy( cfg.getProperties() );
		this.cacheBitResultsSize = ConfigurationParseHelper.getIntValue( cfg.getProperties(), Environment.CACHE_BIT_RESULT_SIZE, CachingWrapperFilter.DEFAULT_SIZE );
		this.barrier = 1; //write barrier
	}

	private static String defineIndexingStrategy(SearchConfiguration cfg) {
		String indexingStrategy = cfg.getProperties().getProperty( Environment.INDEXING_STRATEGY, "event" );
		if ( ! ("event".equals( indexingStrategy ) || "manual".equals( indexingStrategy ) ) ) {
			throw new SearchException( Environment.INDEXING_STRATEGY + " unknown: " + indexingStrategy );
		}
		return indexingStrategy;
	}

	public String getIndexingStrategy() {
		if (barrier != 0) { } //read barrier
		return indexingStrategy;
	}

	public void close() {
		if (barrier != 0) { } //read barrier
		if ( stopped.compareAndSet( false, true) ) {
			try {
				worker.close();
			}
			catch (Exception e) {
				log.error( "Worker raises an exception on close()", e );
			}
			//TODO move to DirectoryProviderFactory for cleaner
			for (DirectoryProvider dp : getDirectoryProviders() ) {
				try {
					dp.stop();
				}
				catch (Exception e) {
					log.error( "DirectoryProvider raises an exception on stop() ", e );
				}
			}
		}
	}

	public void addClassToDirectoryProvider(Class clazz, DirectoryProvider<?> directoryProvider) {
		//no need to set a read barrier, we only use this class in the init thread
		DirectoryProviderData data = dirProviderData.get(directoryProvider);
		if (data == null) {
			data = new DirectoryProviderData();
			dirProviderData.put( directoryProvider, data );
		}
		data.classes.add(clazz);
	}

	public Set<Class> getClassesInDirectoryProvider(DirectoryProvider<?> directoryProvider) {
		if (barrier != 0) { } //read barrier
		return Collections.unmodifiableSet( dirProviderData.get(directoryProvider).classes );
	}

	private void bindFilterDefs(XClass mappedXClass) {
		FullTextFilterDef defAnn = mappedXClass.getAnnotation( FullTextFilterDef.class );
		if ( defAnn != null ) {
			bindFilterDef( defAnn, mappedXClass );
		}
		FullTextFilterDefs defsAnn = mappedXClass.getAnnotation( FullTextFilterDefs.class );
		if (defsAnn != null) {
			for ( FullTextFilterDef def : defsAnn.value() ) {
				bindFilterDef( def, mappedXClass );
			}
		}
	}

	private void bindFilterDef(FullTextFilterDef defAnn, XClass mappedXClass) {
		if ( filterDefinitions.containsKey( defAnn.name() ) ) {
			throw new SearchException("Multiple definition of @FullTextFilterDef.name=" + defAnn.name() + ": "
					+ mappedXClass.getName() );
		}
		FilterDef filterDef = new FilterDef();
		filterDef.setImpl( defAnn.impl() );
		filterDef.setCache( defAnn.cache() );
		filterDef.setUseCachingWrapperFilter( defAnn.cacheBitResult() );
		try {
			filterDef.getImpl().newInstance();
		}
		catch (IllegalAccessException e) {
			throw new SearchException("Unable to create Filter class: " + filterDef.getImpl().getName(), e);
		}
		catch (InstantiationException e) {
			throw new SearchException("Unable to create Filter class: " + filterDef.getImpl().getName(), e);
		}
		for ( Method method : filterDef.getImpl().getMethods() ) {
			if ( method.isAnnotationPresent( Factory.class ) ) {
				if ( filterDef.getFactoryMethod() != null ) {
					throw new SearchException("Multiple @Factory methods found" + defAnn.name() + ": "
							+ filterDef.getImpl().getName() + "." + method.getName() );
				}
				if ( !method.isAccessible() ) method.setAccessible( true );
				filterDef.setFactoryMethod( method );
			}
			if ( method.isAnnotationPresent( Key.class ) ) {
				if ( filterDef.getKeyMethod() != null ) {
					throw new SearchException("Multiple @Key methods found" + defAnn.name() + ": "
							+ filterDef.getImpl().getName() + "." + method.getName() );
				}
				if ( !method.isAccessible() ) method.setAccessible( true );
				filterDef.setKeyMethod( method );
			}

			String name = method.getName();
			if ( name.startsWith( "set" ) && method.getParameterTypes().length == 1 ) {
				filterDef.addSetter( Introspector.decapitalize( name.substring( 3 ) ), method );
			}
		}
		filterDefinitions.put( defAnn.name(), filterDef );
	}


	public Map<Class, DocumentBuilder<Object>> getDocumentBuilders() {
		if (barrier != 0) { } //read barrier
		return documentBuilders;
	}

	public Set<DirectoryProvider> getDirectoryProviders() {
		if (barrier != 0) { } //read barrier
		return this.dirProviderData.keySet();
	}

	public Worker getWorker() {
		if (barrier != 0) { } //read barrier
		return worker;
	}

	public void addOptimizerStrategy(DirectoryProvider<?> provider, OptimizerStrategy optimizerStrategy) {
		//no need to set a read barrier, we run this method on the init thread
		DirectoryProviderData data = dirProviderData.get(provider);
		if (data == null) {
			data = new DirectoryProviderData();
			dirProviderData.put( provider, data );
		}
		data.optimizerStrategy = optimizerStrategy;
	}

	public void addIndexingParameters(DirectoryProvider<?> provider, LuceneIndexingParameters indexingParams) {
		//no need to set a read barrier, we run this method on the init thread
		dirProviderIndexingParams.put( provider, indexingParams );
	}

	public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider) {
		if (barrier != 0) {} //read barrier
		return dirProviderData.get( provider ).optimizerStrategy;
	}

	public LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider ) {
		if (barrier != 0) {} //read barrier
		return dirProviderIndexingParams.get( provider );
	}

	public ReaderProvider getReaderProvider() {
		if (barrier != 0) {} //read barrier
		return readerProvider;
	}

	public DirectoryProvider[] getDirectoryProviders(Class entity) {
		if (barrier != 0) {} //read barrier
		DocumentBuilder<Object> documentBuilder = getDocumentBuilders().get( entity );
		return documentBuilder == null ? null : documentBuilder.getDirectoryProviders();
	}

	public void optimize() {
		if (barrier != 0) {} //read barrier
		Set<Class> clazzs = getDocumentBuilders().keySet();
		for (Class clazz : clazzs) {
			optimize( clazz );
		}
	}

	public void optimize(Class entityType) {
		if (barrier != 0) {} //read barrier
		if ( ! getDocumentBuilders().containsKey( entityType ) ) {
			throw new SearchException("Entity not indexed: " + entityType);
		}
		List<LuceneWork> queue = new ArrayList<LuceneWork>(1);
		queue.add( new OptimizeLuceneWork( entityType ) );
		getBackendQueueProcessorFactory().getProcessor( queue ).run();
	}

	public Analyzer getAnalyzer(String name) {
		if (barrier != 0) {} //read barrier
		final Analyzer analyzer = analyzers.get( name );
		if ( analyzer == null) throw new SearchException( "Unknown Analyzer definition: " + name);
		return analyzer;
	}

	private void initDocumentBuilders(SearchConfiguration cfg, ReflectionManager reflectionManager) {
		InitContext context = new InitContext( cfg );
		Iterator<Class<?>> iter = cfg.getClassMappings();
		DirectoryProviderFactory factory = new DirectoryProviderFactory();

		while ( iter.hasNext() ) {
			Class mappedClass = iter.next();
			if (mappedClass != null) {
				XClass mappedXClass = reflectionManager.toXClass(mappedClass);
				if ( mappedXClass != null) {
					if ( mappedXClass.isAnnotationPresent( Indexed.class ) ) {
						DirectoryProviderFactory.DirectoryProviders providers = factory.createDirectoryProviders( mappedXClass, cfg, this, reflectionManager );

						final DocumentBuilder<Object> documentBuilder = new DocumentBuilder<Object>(
								mappedXClass, context, providers.getProviders(), providers.getSelectionStrategy(),
								reflectionManager
						);

						documentBuilders.put( mappedClass, documentBuilder );
					}
					bindFilterDefs(mappedXClass);
					//TODO should analyzer def for classes at tyher sqme level???
				}
			}
		}
		analyzers = context.initLazyAnalyzers();
		factory.startDirectoryProviders();
	}

	private static FilterCachingStrategy buildFilterCachingStrategy(Properties properties) {
		FilterCachingStrategy filterCachingStrategy;
		String impl = properties.getProperty( Environment.FILTER_CACHING_STRATEGY );
		if ( StringHelper.isEmpty( impl ) || "mru".equalsIgnoreCase( impl ) ) {
			filterCachingStrategy = new MRUFilterCachingStrategy();
		}
		else {
			try {
				Class filterCachingStrategyClass = org.hibernate.annotations.common.util.ReflectHelper.classForName( impl, SearchFactoryImpl.class );
				filterCachingStrategy = (FilterCachingStrategy) filterCachingStrategyClass.newInstance();
			}
			catch (ClassNotFoundException e) {
				throw new SearchException( "Unable to find filterCachingStrategy class: " + impl, e );
			}
			catch (IllegalAccessException e) {
				throw new SearchException( "Unable to instantiate filterCachingStrategy class: " + impl, e );
			}
			catch (InstantiationException e) {
				throw new SearchException( "Unable to instantiate filterCachingStrategy class: " + impl, e );
			}
		}
		filterCachingStrategy.initialize( properties );
		return filterCachingStrategy;
	}

	public FilterCachingStrategy getFilterCachingStrategy() {
		if (barrier != 0) {} //read barrier
		return filterCachingStrategy;
	}

	public FilterDef getFilterDefinition(String name) {
		if (barrier != 0) {} //read barrier
		return filterDefinitions.get( name );
	}

	private static class DirectoryProviderData {
		public final Lock dirLock = new ReentrantLock();
		public OptimizerStrategy optimizerStrategy;
		public Set<Class> classes = new HashSet<Class>(2);
	}

	public Lock getDirectoryProviderLock(DirectoryProvider dp) {
		if (barrier != 0) {} //read barrier
		return this.dirProviderData.get( dp ).dirLock;
	}

	public void addDirectoryProvider(DirectoryProvider<?> provider) {
		//no need to set a barrier we use this method in the init thread
		this.dirProviderData.put( provider, new DirectoryProviderData() );
	}

	public int getFilterCacheBitResultsSize() {
		if (barrier != 0) {} //read barrier
		return cacheBitResultsSize;
	}
}
