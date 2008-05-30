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
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
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
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.SearchFactoryImplementor;
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
	private static final ThreadLocal<WeakHashMap<Configuration, SearchFactoryImpl>> contexts =
			new ThreadLocal<WeakHashMap<Configuration, SearchFactoryImpl>>();

	static {
		Version.touch();
	}

	private static final Logger log = LoggerFactory.getLogger( SearchFactoryImpl.class );

	private final Map<Class, DocumentBuilder<Object>> documentBuilders = new HashMap<Class, DocumentBuilder<Object>>();
	//keep track of the index modifiers per DirectoryProvider since multiple entity can use the same directory provider
	//TODO move the ReentrantLock into DirectoryProviderData.lock, add a getDPLock(DP) and add a Set<DP> getDirectoryProviders() method.
	private final Map<DirectoryProvider, ReentrantLock> lockableDirectoryProviders =
			new HashMap<DirectoryProvider, ReentrantLock>();
	private final Map<DirectoryProvider, DirectoryProviderData> dirProviderData =
			new HashMap<DirectoryProvider, DirectoryProviderData>();
	private Worker worker;
	private ReaderProvider readerProvider;
	private BackendQueueProcessorFactory backendQueueProcessorFactory;
	private final Map<String, FilterDef> filterDefinitions = new HashMap<String, FilterDef>();
	private FilterCachingStrategy filterCachingStrategy;
	private Map<String, Analyzer> analyzers;
	private boolean stopped = false;

	/**
	 * Each directory provider (index) can have its own performance settings.
	 */
	private Map<DirectoryProvider, LuceneIndexingParameters> dirProviderIndexingParams =
		new HashMap<DirectoryProvider, LuceneIndexingParameters>();
	private String indexingStrategy;


	public BackendQueueProcessorFactory getBackendQueueProcessorFactory() {
		return backendQueueProcessorFactory;
	}

	public void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory) {
		this.backendQueueProcessorFactory = backendQueueProcessorFactory;
	}

	@SuppressWarnings( "unchecked" )
	public SearchFactoryImpl(Configuration cfg) {
		//yuk
		ReflectionManager reflectionManager = getReflectionManager( cfg );
		setIndexingStrategy(cfg); //need to be done before the document builds
		InitContext context = new InitContext(cfg);
		initDocumentBuilders(cfg, reflectionManager, context );

		Set<Class> indexedClasses = documentBuilders.keySet();
		for (DocumentBuilder builder : documentBuilders.values()) {
			builder.postInitialize( indexedClasses );
		}
		worker = WorkerFactory.createWorker( cfg, this );
		readerProvider = ReaderProviderFactory.createReaderProvider( cfg, this );
		buildFilterCachingStrategy( cfg.getProperties() );
	}

	private void setIndexingStrategy(Configuration cfg) {
		indexingStrategy = cfg.getProperties().getProperty( Environment.INDEXING_STRATEGY, "event" );
		if ( ! ("event".equals( indexingStrategy ) || "manual".equals( indexingStrategy ) ) ) {
			throw new SearchException(Environment.INDEXING_STRATEGY + " unknown: " + indexingStrategy);
		}
	}

	public String getIndexingStrategy() {
		return indexingStrategy;
	}

	public void close() {
		if (!stopped) {
			stopped = true;
			try {
				worker.close();
			}
			catch (Exception e) {
				log.error( "Worker raises an exception on close()", e );
			}
			//TODO move to DirectoryProviderFactory for cleaner
			for (DirectoryProvider dp : lockableDirectoryProviders.keySet() ) {
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
		DirectoryProviderData data = dirProviderData.get(directoryProvider);
		if (data == null) {
			data = new DirectoryProviderData();
			dirProviderData.put( directoryProvider, data );
		}
		data.classes.add(clazz);
	}

	public Set<Class> getClassesInDirectoryProvider(DirectoryProvider<?> directoryProvider) {
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

	//code doesn't have to be multithreaded because SF creation is not.
	//this is not a public API, should really only be used during the SessionFActory building
	//FIXME this is ugly, impl.staticmethod, fix that
	public static SearchFactoryImpl getSearchFactory(Configuration cfg) {
		WeakHashMap<Configuration, SearchFactoryImpl> contextMap = contexts.get();
		if ( contextMap == null ) {
			contextMap = new WeakHashMap<Configuration, SearchFactoryImpl>( 2 );
			contexts.set( contextMap );
		}
		SearchFactoryImpl searchFactory = contextMap.get( cfg );
		if ( searchFactory == null ) {
			searchFactory = new SearchFactoryImpl( cfg );

			contextMap.put( cfg, searchFactory );
		}
		return searchFactory;
	}


	public Map<Class, DocumentBuilder<Object>> getDocumentBuilders() {
		return documentBuilders;
	}

	public Map<DirectoryProvider, ReentrantLock> getLockableDirectoryProviders() {
		return lockableDirectoryProviders;
	}

	public Worker getWorker() {
		return worker;
	}

	public void addOptimizerStrategy(DirectoryProvider<?> provider, OptimizerStrategy optimizerStrategy) {
		DirectoryProviderData data = dirProviderData.get(provider);
		if (data == null) {
			data = new DirectoryProviderData();
			dirProviderData.put( provider, data );
		}
		data.optimizerStrategy = optimizerStrategy;
	}

	public void addIndexingParameters(DirectoryProvider<?> provider, LuceneIndexingParameters indexingParams) {
		dirProviderIndexingParams.put( provider, indexingParams );
	}

	public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider) {
		return dirProviderData.get( provider ).optimizerStrategy;
	}

	public LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider ) {
		return dirProviderIndexingParams.get( provider );
	}

	public ReaderProvider getReaderProvider() {
		return readerProvider;
	}

	//not happy about having it as a helper class but I don't want cfg to be associated with the SearchFactory
	public static ReflectionManager getReflectionManager(Configuration cfg) {
		ReflectionManager reflectionManager;
		try {
			//TODO introduce a ReflectionManagerHolder interface to avoid reflection
			//I want to avoid hard link between HAN and Validator for usch a simple need
			//reuse the existing reflectionManager one when possible
			reflectionManager =
					(ReflectionManager) cfg.getClass().getMethod( "getReflectionManager" ).invoke( cfg );

		}
		catch (Exception e) {
			reflectionManager = new JavaReflectionManager();
		}
		return reflectionManager;
	}

	public DirectoryProvider[] getDirectoryProviders(Class entity) {
		DocumentBuilder<Object> documentBuilder = getDocumentBuilders().get( entity );
		return documentBuilder == null ? null : documentBuilder.getDirectoryProviders();
	}

	public void optimize() {
		Set<Class> clazzs = getDocumentBuilders().keySet();
		for (Class clazz : clazzs) {
			optimize( clazz );
		}
	}

	public void optimize(Class entityType) {
		if ( ! getDocumentBuilders().containsKey( entityType ) ) {
			throw new SearchException("Entity not indexed: " + entityType);
		}
		List<LuceneWork> queue = new ArrayList<LuceneWork>(1);
		queue.add( new OptimizeLuceneWork( entityType ) );
		getBackendQueueProcessorFactory().getProcessor( queue ).run();
	}

	public Analyzer getAnalyzer(String name) {
		final Analyzer analyzer = analyzers.get( name );
		if ( analyzer == null) throw new SearchException( "Unknown Analyzer definition: " + name);
		return analyzer;
	}

	private void initDocumentBuilders(Configuration cfg, ReflectionManager reflectionManager, InitContext context) {
		Iterator iter = cfg.getClassMappings();
		DirectoryProviderFactory factory = new DirectoryProviderFactory();

		while (iter.hasNext()) {
			PersistentClass clazz = (PersistentClass) iter.next();
			Class<?> mappedClass = clazz.getMappedClass();
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

	private void buildFilterCachingStrategy(Properties properties) {
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
	}

	public FilterCachingStrategy getFilterCachingStrategy() {
		return filterCachingStrategy;
	}

	public FilterDef getFilterDefinition(String name) {
		return filterDefinitions.get( name );
	}

	private static class DirectoryProviderData {
		public OptimizerStrategy optimizerStrategy;
		public Set<Class> classes = new HashSet<Class>(2);
	}
}
