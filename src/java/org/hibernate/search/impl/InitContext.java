// $Id:$
package org.hibernate.search.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.SearchException;
import org.hibernate.search.Environment;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.util.DelegateNamedAnalyzer;
import org.hibernate.util.ReflectHelper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.Similarity;
import org.apache.solr.analysis.TokenizerFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.analysis.TokenFilterFactory;

/**
 * @author Emmanuel Bernard
 */
public class InitContext {
	private final Map<String, AnalyzerDef> analyzerDefs = new HashMap<String, AnalyzerDef>();
	private final List<DelegateNamedAnalyzer> lazyAnalyzers = new ArrayList<DelegateNamedAnalyzer>();
	private final Analyzer defaultAnalyzer;
	private final Similarity defaultSimilarity;

	public InitContext(SearchConfiguration cfg) {
		defaultAnalyzer = initAnalyzer(cfg);
		defaultSimilarity = initSimilarity(cfg);
	}

	public void addAnalyzerDef(AnalyzerDef ann) {
		//FIXME somehow remember where the analyzerDef comes from and raise an exception if an analyzerDef
		//with the same name from two different places are added
		//multiple adding from the same place is required to deal with inheritance hierarchy processed multiple times
		if ( ann != null && analyzerDefs.put( ann.name(), ann ) != null ) {
			//throw new SearchException("Multiple AnalyzerDef with the same name: " + name);
		}
	}

	public Analyzer buildLazyAnalyzer(String name) {
		final DelegateNamedAnalyzer delegateNamedAnalyzer = new DelegateNamedAnalyzer( name );
		lazyAnalyzers.add(delegateNamedAnalyzer);
		return delegateNamedAnalyzer;
	}

	public List<DelegateNamedAnalyzer> getLazyAnalyzers() {
		return lazyAnalyzers;
	}

	/**
	 * Initializes the Lucene analyzer to use by reading the analyzer class from the configuration and instantiating it.
	 *
	 * @param cfg
	 *            The current configuration.
	 * @return The Lucene analyzer to use for tokenisation.
	 */
	private Analyzer initAnalyzer(SearchConfiguration cfg) {
		Class analyzerClass;
		String analyzerClassName = cfg.getProperty( Environment.ANALYZER_CLASS);
		if (analyzerClassName != null) {
			try {
				analyzerClass = ReflectHelper.classForName(analyzerClassName);
			} catch (Exception e) {
				return buildLazyAnalyzer( analyzerClassName );
//				throw new SearchException("Lucene analyzer class '" + analyzerClassName + "' defined in property '"
//						+ Environment.ANALYZER_CLASS + "' could not be found.", e);
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

	/**
	 * Initializes the Lucene similarity to use
	 */
	private Similarity initSimilarity(SearchConfiguration cfg) {
		Class similarityClass;
		String similarityClassName = cfg.getProperty(Environment.SIMILARITY_CLASS);
		if (similarityClassName != null) {
			try {
				similarityClass = ReflectHelper.classForName(similarityClassName);
			} catch (Exception e) {
				throw new SearchException("Lucene Similarity class '" + similarityClassName + "' defined in property '"
						+ Environment.SIMILARITY_CLASS + "' could not be found.", e);
			}
		}
		else {
			similarityClass = null;
		}

		// Initialize similarity
		if ( similarityClass == null ) {
			return Similarity.getDefault();
		}
		else {
			Similarity defaultSimilarity;
			try {
				defaultSimilarity = (Similarity) similarityClass.newInstance();
			} catch (ClassCastException e) {
				throw new SearchException("Lucene similarity does not extend " + Similarity.class.getName() + ": "
						+ similarityClassName, e);
			} catch (Exception e) {
				throw new SearchException("Failed to instantiate lucene similarity with type " + similarityClassName, e);
			}
			return defaultSimilarity;
		}
	}

	public Analyzer getDefaultAnalyzer() {
		return defaultAnalyzer;
	}

	public Similarity getDefaultSimilarity() {
		return defaultSimilarity;
	}

	public Map<String, Analyzer> initLazyAnalyzers() {
		Map<String, Analyzer> initializedAnalyzers = new HashMap<String, Analyzer>( analyzerDefs.size() );

		for (DelegateNamedAnalyzer namedAnalyzer : lazyAnalyzers) {
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
					throw new SearchException("Analyzer found with an unknown definition: " + name);
				}
			}
		}

		//initialize the remaining definitions
		for ( Map.Entry<String, AnalyzerDef> entry : analyzerDefs.entrySet() ) {
			if ( ! initializedAnalyzers.containsKey( entry.getKey() ) ) {
				final Analyzer analyzer = buildAnalyzer( entry.getValue() );
				initializedAnalyzers.put( entry.getKey(), analyzer );
			}
		}
		return Collections.unmodifiableMap( initializedAnalyzers );
	}

	private Analyzer buildAnalyzer(AnalyzerDef analyzerDef) {
		TokenizerDef token = analyzerDef.tokenizer();
		TokenizerFactory tokenFactory = (TokenizerFactory) instantiate( token.factory() );
		tokenFactory.init( getMapOfParameters( token.params() ) );

		final int length = analyzerDef.filters().length;
		TokenFilterFactory[] filters = new TokenFilterFactory[length];
		for ( int index = 0 ; index < length ; index++ ) {
			TokenFilterDef filterDef = analyzerDef.filters()[index];
			filters[index] = (TokenFilterFactory) instantiate( filterDef.factory() );
			filters[index].init( getMapOfParameters( filterDef.params() ) );
		}
		return new TokenizerChain(tokenFactory, filters);
	}

	private Object instantiate(Class clazz) {
		try {
			return clazz.newInstance();
		}
		catch (IllegalAccessException e) {
			throw new SearchException( "Unable to instantiate class: " + clazz, e );
		}
		catch (InstantiationException e) {
			throw new SearchException( "Unable to instantiate class: " + clazz, e );
		}
	}

	private Map<String, String> getMapOfParameters(Parameter[] params) {
		Map<String, String> mapOfParams = new HashMap<String, String>( params.length );
		for (Parameter param : params) {
			mapOfParams.put( param.name(), param.value() );
		}
		return Collections.unmodifiableMap( mapOfParams );
	}
}
