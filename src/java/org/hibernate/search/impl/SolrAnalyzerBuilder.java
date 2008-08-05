package org.hibernate.search.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerFactory;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.SearchException;

/**
 * This class has a direct dependency on Solr classes
 *
 * @author Emmanuel Bernard
 */
class SolrAnalyzerBuilder {
	private SolrAnalyzerBuilder() {}

	public static Analyzer buildAnalyzer(AnalyzerDef analyzerDef) {
		TokenizerDef token = analyzerDef.tokenizer();
		TokenizerFactory tokenFactory = ( TokenizerFactory ) instantiate( token.factory() );
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
