/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;


/**
 * @author Yoann Rodiere
 */
public class LuceneEmbeddedAnalyzerStrategy implements AnalyzerStrategy<LuceneAnalyzerReference> {

	private static final Log log = LoggerFactory.make();

	private final ServiceManager serviceManager;

	private final SearchConfiguration cfg;

	private final Version luceneMatchVersion;

	public LuceneEmbeddedAnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		this.serviceManager = serviceManager;
		this.cfg = cfg;
		this.luceneMatchVersion = getLuceneMatchVersion( cfg );
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
		return createAnalyzerReference( analyzerClass );
	}

	@Override
	public LuceneAnalyzerReference createPassThroughAnalyzerReference() {
		return new LuceneAnalyzerReference( PassThroughAnalyzer.INSTANCE );
	}

	@Override
	public LuceneAnalyzerReference createAnalyzerReference(Class<?> analyzerClass) {
		try {
			Analyzer analyzer = ClassLoaderHelper.analyzerInstanceFromClass( analyzerClass, luceneMatchVersion );
			return new LuceneAnalyzerReference( analyzer );
		}
		catch (ClassCastException e) {
			throw new SearchException( "Lucene analyzer does not extend " + Analyzer.class.getName() + ": " + analyzerClass.getName(), e );
		}
		catch (Exception e) {
			throw new SearchException( "Failed to instantiate lucene analyzer with type " + analyzerClass.getName(), e );
		}
	}

	@Override
	public LuceneAnalyzerReference createNamedAnalyzerReference(String name) {
		return new LuceneAnalyzerReference( name );
	}

	@Override
	public void initializeNamedAnalyzerReferences(Map<String, LuceneAnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		Map<String, Analyzer> initializedAnalyzers = new HashMap<>();
		for ( Map.Entry<String, LuceneAnalyzerReference> entry : references.entrySet() ) {
			initializeReference( initializedAnalyzers, entry.getKey(), entry.getValue(), analyzerDefinitions );
		}
	}

	private void initializeReference(Map<String, Analyzer> initializedAnalyzers, String name, LuceneAnalyzerReference analyzerReference,
			Map<String, AnalyzerDef> analyzerDefinitions) {
		if ( analyzerReference.isInitialized() ) {
			initializedAnalyzers.put( analyzerReference.getAnalyzerName(), analyzerReference.getAnalyzer() );
			return;
		}

		Analyzer analyzer = initializedAnalyzers.get( name );

		if ( analyzer == null ) {
			AnalyzerDef analyzerDefinition = analyzerDefinitions.get( name );
			if ( analyzerDefinition == null ) {
				throw new SearchException( "Lucene analyzer found with an unknown definition: " + name );
			}
			analyzer = buildAnalyzer( analyzerDefinition );
			initializedAnalyzers.put( name, analyzer );
		}

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
	public ScopedLuceneAnalyzer.Builder buildScopedAnalyzer(LuceneAnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedLuceneAnalyzer.Builder( initialGlobalAnalyzerReference, Collections.<String, LuceneAnalyzerReference>emptyMap() );
	}
}
