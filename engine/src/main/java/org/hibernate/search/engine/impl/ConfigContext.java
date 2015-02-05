/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.cfg.EntityDescriptor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.DelegateNamedAnalyzer;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Provides access to some default configuration settings (eg default {@code Analyzer} or default
 * {@code Similarity}) and checks whether certain optional libraries are available.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ConfigContext {

	private static final Log log = LoggerFactory.make();

	/**
	 * The default token for indexing null values. See {@link org.hibernate.search.annotations.Field#indexNullAs()}
	 */
	private static final String DEFAULT_NULL_INDEX_TOKEN = "_null_";

	/**
	 * Constant used as definition point for a global (programmatic) analyzer definition. In this case no annotated
	 * element is available to be used as definition point.
	 */
	private static final String PROGRAMMATIC_ANALYZER_DEFINITION = "PROGRAMMATIC_ANALYZER_DEFINITION";

	/**
	 * Constant used as definition point for a global (programmatic) filter definition. In this case no annotated
	 * element is available to be used as definition point.
	 */
	private static final String PROGRAMMATIC_FILTER_DEFINITION = "PROGRAMMATIC_FILTER_DEFINITION";

	/**
	 * Used to keep track of duplicated analyzer definitions. The key of the map is the analyzer definition
	 * name and the value is a string defining the location of the definition. In most cases the fully specified class
	 * name together with the annotated element name is used. See also {@link #PROGRAMMATIC_ANALYZER_DEFINITION}.
	 */
	private final Map<String, String> analyzerDefinitionPoints = new HashMap<String, String>();

	/**
	 * Used to keep track of duplicated filter definitions. The key of the map is the filter definition
	 * name and the value is a string defining the location of the definition. In most cases the fully specified class
	 * name together with the annotated element name is used.
	 */
	private final Map<String, String> filterDefinitionPoints = new HashMap<String, String>();

	/**
	 * Map of discovered analyzer definitions. The key of the map is the analyzer def name and the value is the
	 * {@code AnalyzerDef} annotation.
	 */
	private final Map<String, AnalyzerDef> analyzerDefs = new HashMap<String, AnalyzerDef>();

	/**
	 * Map of discovered filter definitions. The key of the map is the filter def name and the value is the
	 * {@code FilterDef} instance.
	 */
	private final Map<String, FilterDef> filterDefs = new HashMap<String, FilterDef>();

	private final List<DelegateNamedAnalyzer> lazyAnalyzers = new ArrayList<DelegateNamedAnalyzer>();
	private final Analyzer defaultAnalyzer;
	private final boolean jpaPresent;
	private final Version luceneMatchVersion;
	private final String nullToken;
	private final boolean implicitProvidedId;
	private final SearchMapping searchMapping;
	private final ServiceManager serviceManager;

	public ConfigContext(SearchConfiguration searchConfiguration, BuildContext buildContext) {
		this( searchConfiguration, buildContext, null );
	}

	public ConfigContext(SearchConfiguration searchConfiguration, BuildContext buildContext, SearchMapping searchMapping) {
		this.serviceManager = buildContext.getServiceManager();
		this.luceneMatchVersion = getLuceneMatchVersion( searchConfiguration );
		this.defaultAnalyzer = initAnalyzer( searchConfiguration );
		this.jpaPresent = isPresent( "javax.persistence.Id" );
		this.nullToken = initNullToken( searchConfiguration );
		this.implicitProvidedId = searchConfiguration.isIdProvidedImplicit();
		this.searchMapping = searchMapping;
	}

	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	/**
	 * Add an analyzer definition which was defined as annotation.
	 *
	 * @param analyzerDef the analyzer definition annotation
	 * @param annotatedElement the annotated element it was defined on
	 */
	public void addAnalyzerDef(AnalyzerDef analyzerDef, XAnnotatedElement annotatedElement) {
		if ( analyzerDef == null ) {
			return;
		}
		addAnalyzerDef( analyzerDef, buildAnnotationDefinitionPoint( annotatedElement ) );
	}

	/** Add a full-filter definition which was defined as annotation
	 *
	 * @param filterDef the filter defition annotation
	 * @param annotatedElement the annotated element it was defined on
	 */
	public void addFullTextFilterDef(FullTextFilterDef filterDef, XAnnotatedElement annotatedElement) {
		if ( filterDef == null ) {
			return;
		}
		addFullTextFilterDef( filterDef, buildAnnotationDefinitionPoint( annotatedElement ) );
	}

	public void addGlobalAnalyzerDef(AnalyzerDef analyzerDef) {
		addAnalyzerDef( analyzerDef, PROGRAMMATIC_ANALYZER_DEFINITION );
	}

	public void addGlobalFullTextFilterDef(FullTextFilterDef filterDef) {
		addFullTextFilterDef( filterDef, PROGRAMMATIC_FILTER_DEFINITION );
	}

	private void addAnalyzerDef(AnalyzerDef analyzerDef, String annotationDefinitionPoint) {
		String analyzerDefinitionName = analyzerDef.name();

		if ( analyzerDefinitionPoints.containsKey( analyzerDefinitionName ) ) {
			if ( !analyzerDefinitionPoints.get( analyzerDefinitionName ).equals( annotationDefinitionPoint ) ) {
				throw new SearchException( "Multiple analyzer definitions with the same name: " + analyzerDef.name() );
			}
		}
		else {
			analyzerDefs.put( analyzerDefinitionName, analyzerDef );
			analyzerDefinitionPoints.put( analyzerDefinitionName, annotationDefinitionPoint );
		}
	}

	public Analyzer buildLazyAnalyzer(String name) {
		final DelegateNamedAnalyzer delegateNamedAnalyzer = new DelegateNamedAnalyzer( name );
		lazyAnalyzers.add( delegateNamedAnalyzer );
		return delegateNamedAnalyzer;
	}

	/**
	 * Initializes the Lucene analyzer to use by reading the analyzer class from the configuration and instantiating it.
	 *
	 * @param cfg The current configuration.
	 *
	 * @return The Lucene analyzer to use for tokenization.
	 */
	private Analyzer initAnalyzer(SearchConfiguration cfg) {
		Class analyzerClass;
		String analyzerClassName = cfg.getProperty( Environment.ANALYZER_CLASS );
		if ( analyzerClassName != null ) {
			try {
				analyzerClass = ClassLoaderHelper.classForName( analyzerClassName, serviceManager );
			}
			catch (Exception e) {
				return buildLazyAnalyzer( analyzerClassName );
			}
		}
		else {
			analyzerClass = StandardAnalyzer.class;
		}
		return ClassLoaderHelper.analyzerInstanceFromClass( analyzerClass, luceneMatchVersion );
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

	public Analyzer getDefaultAnalyzer() {
		return defaultAnalyzer;
	}

	public Version getLuceneMatchVersion() {
		return luceneMatchVersion;
	}

	private void addFullTextFilterDef(FullTextFilterDef filterDef, String filterDefinitionPoint) {
		String filterDefinitionName = filterDef.name();

		if ( filterDefinitionPoints.containsKey( filterDefinitionName ) ) {
			if ( !filterDefinitionPoints.get( filterDefinitionName ).equals( filterDefinitionPoint ) ) {
				throw new SearchException( "Multiple filter definitions with the same name: " + filterDef.name() );
			}
		}
		else {
			filterDefinitionPoints.put( filterDefinitionName, filterDefinitionPoint );
			addFilterDef( filterDef );
		}
	}

	private void addFilterDef(FullTextFilterDef defAnn) {
		FilterDef filterDef = new FilterDef( defAnn );
		if ( filterDef.getImpl().equals( ShardSensitiveOnlyFilter.class ) ) {
			//this is a placeholder don't process regularly
			filterDefs.put( defAnn.name(), filterDef );
			return;
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
		filterDefs.put( defAnn.name(), filterDef );
	}

	public Map<String, Analyzer> initLazyAnalyzers() {
		Map<String, Analyzer> initializedAnalyzers = new HashMap<String, Analyzer>( analyzerDefs.size() );

		for ( DelegateNamedAnalyzer namedAnalyzer : lazyAnalyzers ) {
			String name = namedAnalyzer.getName();
			if ( initializedAnalyzers.containsKey( name ) ) {
				namedAnalyzer.setDelegate( initializedAnalyzers.get( name ) );
			}
			else {
				if ( analyzerDefs.containsKey( name ) ) {
					final Analyzer analyzer = buildAnalyzer( analyzerDefs.get( name ) );
					namedAnalyzer.setDelegate( analyzer );
					initializedAnalyzers.put( name, analyzer );
				}
				else {
					throw new SearchException( "Analyzer found with an unknown definition: " + name );
				}
			}
		}

		//initialize the remaining definitions
		for ( Map.Entry<String, AnalyzerDef> entry : analyzerDefs.entrySet() ) {
			if ( !initializedAnalyzers.containsKey( entry.getKey() ) ) {
				final Analyzer analyzer = buildAnalyzer( entry.getValue() );
				initializedAnalyzers.put( entry.getKey(), analyzer );
			}
		}
		return Collections.unmodifiableMap( initializedAnalyzers );
	}

	public Map<String, FilterDef> initFilters() {
		return Collections.unmodifiableMap( filterDefs );
	}

	private Analyzer buildAnalyzer(AnalyzerDef analyzerDef) {
		try {
			return SolrAnalyzerBuilder.buildAnalyzer( analyzerDef, luceneMatchVersion, serviceManager );
		}
		catch (IOException e) {
			throw new SearchException( "Could not initializer Analyzer definition" + analyzerDef, e );
		}
	}

	public boolean isJpaPresent() {
		return jpaPresent;
	}

	private boolean isPresent(String className) {
		try {
			ClassLoaderHelper.classForName( className, serviceManager );
			return true;
		}
		catch (Exception e) {
			return false;
		}
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

	/**
	 * @param annotatedElement an annotated element
	 *
	 * @return a string which identifies the location/point the annotation was placed on. Something of the
	 * form package.[[className].[field|member]]
	 */
	private String buildAnnotationDefinitionPoint(XAnnotatedElement annotatedElement) {
		if ( annotatedElement instanceof XClass ) {
			return ( (XClass) annotatedElement ).getName();
		}
		else if ( annotatedElement instanceof XMember ) {
			XMember member = (XMember) annotatedElement;
			return member.getType().getName() + '.' + member.getName();
		}
		else if ( annotatedElement instanceof XPackage ) {
			return ( (XPackage) annotatedElement ).getName();
		}
		else {
			throw new SearchException( "Unknown XAnnotatedElement: " + annotatedElement );
		}
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
