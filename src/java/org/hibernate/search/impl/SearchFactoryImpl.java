//$Id$
package org.hibernate.search.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.Method;
import java.beans.Introspector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.filter.MRUFilterCachingStrategy;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.backend.WorkerFactory;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.reader.ReaderProviderFactory;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.DirectoryProviderFactory;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.util.ReflectHelper;

/**
 * @author Emmanuel Bernard
 */
public class SearchFactoryImpl implements SearchFactoryImplementor {
	private static ThreadLocal<WeakHashMap<Configuration, SearchFactoryImpl>> contexts =
			new ThreadLocal<WeakHashMap<Configuration, SearchFactoryImpl>>();

	static {
		Version.touch();
	}

	private Map<Class, DocumentBuilder<Object>> documentBuilders = new HashMap<Class, DocumentBuilder<Object>>();
	//keep track of the index modifiers per DirectoryProvider since multiple entity can use the same directory provider
	private Map<DirectoryProvider, ReentrantLock> lockableDirectoryProviders =
			new HashMap<DirectoryProvider, ReentrantLock>();
	private Map<DirectoryProvider, OptimizerStrategy> dirProviderOptimizerStrategies =
			new HashMap<DirectoryProvider, OptimizerStrategy>();
	private Worker worker;
	private ReaderProvider readerProvider;
	private BackendQueueProcessorFactory backendQueueProcessorFactory;
	private Map<String, FilterDef> filterDefinitions = new HashMap<String, FilterDef>();
	private FilterCachingStrategy filterCachingStrategy;

	/**
	 * Each directory provider (index) can have its own performance settings.
	 */
	private Map<DirectoryProvider, LuceneIndexingParameters> dirProviderIndexingParams =
		new HashMap<DirectoryProvider, LuceneIndexingParameters>();


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

		Analyzer analyzer = initAnalyzer(cfg);
		initDocumentBuilders(cfg, reflectionManager, analyzer);

		Set<Class> indexedClasses = documentBuilders.keySet();
		for (DocumentBuilder builder : documentBuilders.values()) {
			builder.postInitialize( indexedClasses );
		}
		worker = WorkerFactory.createWorker( cfg, this );
		readerProvider = ReaderProviderFactory.createReaderProvider( cfg, this );
		buildFilterCachingStrategy( cfg.getProperties() );
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
		dirProviderOptimizerStrategies.put( provider, optimizerStrategy );
	}

	public void addIndexingParmeters(DirectoryProvider<?> provider, LuceneIndexingParameters indexingParams) {
		dirProviderIndexingParams.put( provider, indexingParams );
	}

	public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider) {
		return dirProviderOptimizerStrategies.get( provider );
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

	private void initDocumentBuilders(Configuration cfg, ReflectionManager reflectionManager, Analyzer analyzer) {
		Iterator iter = cfg.getClassMappings();
		DirectoryProviderFactory factory = new DirectoryProviderFactory();
		while (iter.hasNext()) {
			PersistentClass clazz = (PersistentClass) iter.next();
			Class<?> mappedClass = clazz.getMappedClass();
			if (mappedClass != null) {
				XClass mappedXClass = reflectionManager.toXClass(mappedClass);
				if ( mappedXClass != null) {
					if ( mappedXClass.isAnnotationPresent( Indexed.class ) ) {
						DirectoryProviderFactory.DirectoryProviders providers = factory.createDirectoryProviders( mappedXClass, cfg, this );

						final DocumentBuilder<Object> documentBuilder = new DocumentBuilder<Object>(
								mappedXClass, analyzer, providers.getProviders(), providers.getSelectionStrategy(),
								reflectionManager
						);

						documentBuilders.put( mappedClass, documentBuilder );
					}
					bindFilterDefs(mappedXClass);
				}
			}
		}
		factory.startDirectoryProviders();
	}

	/**
	 * Initilises the Lucene analyzer to use by reading the analyzer class from the configuration and instantiating it.
	 *
	 * @param cfg
	 *            The current configuration.
	 * @return The Lucene analyzer to use for tokenisation.
	 */
	private Analyzer initAnalyzer(Configuration cfg) {
		Class analyzerClass;
		String analyzerClassName = cfg.getProperty(Environment.ANALYZER_CLASS);
		if (analyzerClassName != null) {
			try {
				analyzerClass = ReflectHelper.classForName(analyzerClassName);
			} catch (Exception e) {
				throw new SearchException("Lucene analyzer class '" + analyzerClassName + "' defined in property '"
						+ Environment.ANALYZER_CLASS + "' could not be found.", e);
			}
		} else {
			analyzerClass = StandardAnalyzer.class;
		}
		// Initialize analyzer
		Analyzer defaultAnalyzer;
		try {
			defaultAnalyzer = (Analyzer) analyzerClass.newInstance();
		} catch (ClassCastException e) {
			throw new SearchException("Lucene analyzer does not implement " + Analyzer.class.getName() + ": "
					+ analyzerClassName, e);
		} catch (Exception e) {
			throw new SearchException("Failed to instantiate lucene analyzer with type " + analyzerClassName, e);
		}
		return defaultAnalyzer;
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
				throw new SearchException( "Unable to instanciate filterCachingStrategy class: " + impl, e );
			}
			catch (InstantiationException e) {
				throw new SearchException( "Unable to instanciate filterCachingStrategy class: " + impl, e );
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
}
