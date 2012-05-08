/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.Version;

import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.DelegateNamedAnalyzer;
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
	 * Used to keep track of duplicated analyzer definitions. The key of the map is the analyzer definition
	 * name and the value is a string defining the location of the definition. In most cases the fully specified class
	 * name together with the annotated element name is used. See also {@link #PROGRAMMATIC_ANALYZER_DEFINITION}.
	 */
	private final Map<String, String> analyzerDefinitionPoints = new HashMap<String, String>();

	/**
	 * Map of discovered analyzer definitions. The key of the map is the analyzer def name and the value is the
	 * {@code AnalyzerDef} annotation.
	 */
	private final Map<String, AnalyzerDef> analyzerDefs = new HashMap<String, AnalyzerDef>();

	/**
	 * Set of field names used for faceting
	 */
	// todo - Check whether I have to take care about which class defines the name
	private final Collection<String> facetFieldNames = new HashSet<String>();

	private final List<DelegateNamedAnalyzer> lazyAnalyzers = new ArrayList<DelegateNamedAnalyzer>();
	private final Analyzer defaultAnalyzer;
	private final Similarity defaultSimilarity;
	private final boolean solrPresent;
	private final boolean jpaPresent;
	private final Version luceneMatchVersion;
	private final String nullToken;

	public ConfigContext(SearchConfiguration cfg) {
		luceneMatchVersion = getLuceneMatchVersion( cfg );
		defaultAnalyzer = initAnalyzer( cfg );
		defaultSimilarity = initSimilarity( cfg );
		solrPresent = isPresent( "org.apache.solr.analysis.TokenizerFactory" );
		jpaPresent = isPresent( "javax.persistence.Id" );
		nullToken = initNullToken( cfg );
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

	/**
	 * Add a facet field name.
	 *
	 * @param facetFieldName the name of the faceted field
	 */
	public void addFacetFieldName(String facetFieldName) {
		facetFieldNames.add( facetFieldName );
	}

	public Collection<String> getFacetFieldNames() {
		return facetFieldNames;
	}

	public void addGlobalAnalyzerDef(AnalyzerDef analyzerDef) {
		addAnalyzerDef( analyzerDef, PROGRAMMATIC_ANALYZER_DEFINITION );
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

	public List<DelegateNamedAnalyzer> getLazyAnalyzers() {
		return lazyAnalyzers;
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
				// Use the same class loader used to load the SearchConfiguration implementation class ...
				analyzerClass = ClassLoaderHelper.classForName( analyzerClassName, cfg.getClass().getClassLoader() );
			}
			catch ( Exception e ) {
				return buildLazyAnalyzer( analyzerClassName );
			}
		}
		else {
			analyzerClass = StandardAnalyzer.class;
		}
		return ClassLoaderHelper.analyzerInstanceFromClass( analyzerClass, luceneMatchVersion );
	}

	/**
	 * Initializes the Lucene similarity to use.
	 *
	 * @param cfg the search configuration.
	 *
	 * @return returns the default similarity class.
	 */
	private Similarity initSimilarity(SearchConfiguration cfg) {
		String similarityClassName = cfg.getProperty( Environment.SIMILARITY_CLASS );
		Similarity defaultSimilarity;
		if ( StringHelper.isEmpty( similarityClassName ) ) {
			defaultSimilarity = Similarity.getDefault();
		}
		else {
			defaultSimilarity = ClassLoaderHelper.instanceFromName(
					Similarity.class, similarityClassName, ConfigContext.class, "default similarity"
			);
		}
		log.debugf( "Using default similarity implementation: %s", defaultSimilarity.getClass().getName() );
		return defaultSimilarity;
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

	public Similarity getDefaultSimilarity() {
		return defaultSimilarity;
	}

	public Version getLuceneMatchVersion() {
		return luceneMatchVersion;
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

	private Analyzer buildAnalyzer(AnalyzerDef analyzerDef) {
		if ( !solrPresent ) {
			throw new SearchException(
					"Use of @AnalyzerDef while Solr is not present in the classpath. Add apache-solr-analyzer.jar"
			);
		}

		// SolrAnalyzerBuilder references Solr classes.
		// InitContext should not (directly or indirectly) load a Solr class to avoid hard dependency
		// unless necessary
		// the current mechanism (check Solr class presence and call SolrAnalyzerBuilder if needed
		// seems to be sufficient on Apple VM (derived from Sun's
		return SolrAnalyzerBuilder.buildAnalyzer( analyzerDef, luceneMatchVersion );
	}

	public boolean isJpaPresent() {
		return jpaPresent;
	}

	private boolean isPresent(String className) {
		try {
			ClassLoaderHelper.classForName( className, ConfigContext.class.getClassLoader() );
			return true;
		}
		catch ( Exception e ) {
			return false;
		}
	}

	private Version getLuceneMatchVersion(SearchConfiguration cfg) {
		Version version;
		String tmp = cfg.getProperty( Environment.LUCENE_MATCH_VERSION );
		if ( StringHelper.isEmpty( tmp ) ) {
			log.recommendConfiguringLuceneVersion();
			version = Environment.DEFAULT_LUCENE_MATCH_VERSION;
		}
		else {
			try {
				version = Version.valueOf( tmp );
				if ( log.isDebugEnabled() ) {
					log.debug( "Setting Lucene compatibility to Version " + version.name() );
				}
			}
			catch ( IllegalArgumentException e ) {
				StringBuilder msg = new StringBuilder( tmp );
				msg.append( " is a invalid value for the Lucene match version. Possible values are: " );
				for ( Version v : Version.values() ) {
					msg.append( v.toString() );
					msg.append( ", " );
				}
				msg.delete( msg.lastIndexOf( "," ), msg.length() - 1 );
				throw new SearchException( msg.toString() );
			}
		}
		return version;
	}

	/**
	 * @param annotatedElement an annotated element
	 *
	 * @return a string which identifies the location/point the annotation was placed on. Something of the
	 *         form package.[[className].[field|member]]
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
			throw new SearchException( "Unknown XAnnoatedElement: " + annotatedElement );
		}
	}
}
