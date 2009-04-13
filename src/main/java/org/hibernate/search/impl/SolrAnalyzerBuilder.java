package org.hibernate.search.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerFactory;
import org.apache.solr.util.plugin.ResourceLoaderAware;
import org.apache.solr.common.ResourceLoader;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.HibernateSearchResourceLoader;

/**
 * Instances of this class are used to build Lucene analyzers which are defined using the solr <code>TokenFilterFactory</code>.
 * To make the dependency to the solr framework optional only this class has direct dependecies to solr. Solr dependencies
 * are not supposed to be used anywhere else (except the actual configuration of the analyzers in the domain model).
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class SolrAnalyzerBuilder {
	private SolrAnalyzerBuilder() {}

	/**
	 * Builds a Lucene <code>Analyzer</code> from the specified <code>AnalyzerDef</code> annotation.
	 *
	 * @param analyzerDef The <code>AnalyzerDef</code> annotation as found in the annotated domain class.
	 * @return a Lucene <code>Analyzer</code>
	 */
	public static Analyzer buildAnalyzer(AnalyzerDef analyzerDef) {
		TokenizerDef token = analyzerDef.tokenizer();
		TokenizerFactory tokenFactory = ( TokenizerFactory ) instantiate( token.factory() );
		tokenFactory.init( getMapOfParameters( token.params() ) );

		final int length = analyzerDef.filters().length;
		TokenFilterFactory[] filters = new TokenFilterFactory[length];
		ResourceLoader resourceLoader = new HibernateSearchResourceLoader();
		for ( int index = 0 ; index < length ; index++ ) {
			TokenFilterDef filterDef = analyzerDef.filters()[index];
			filters[index] = (TokenFilterFactory) instantiate( filterDef.factory() );
			filters[index].init( getMapOfParameters( filterDef.params() ) );
			if ( filters[index] instanceof ResourceLoaderAware ) {
				((ResourceLoaderAware)filters[index]).inform( resourceLoader );
			}
		}
		return new TokenizerChain(tokenFactory, filters);
	}

	private static Object instantiate(Class clazz) {
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

	private static Map<String, String> getMapOfParameters(Parameter[] params) {
		Map<String, String> mapOfParams = new HashMap<String, String>( params.length );
		for (Parameter param : params) {
			mapOfParams.put( param.name(), param.value() );
		}
		return Collections.unmodifiableMap( mapOfParams );
	}
}
