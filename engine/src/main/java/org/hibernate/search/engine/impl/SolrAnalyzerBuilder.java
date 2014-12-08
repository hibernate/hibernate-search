/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.util.impl.HibernateSearchResourceLoader;

import static org.hibernate.search.util.impl.ClassLoaderHelper.instanceFromClass;

/**
 * Instances of this class are used to build Lucene analyzers which are defined using a <code>TokenFilterFactory</code>.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
final class SolrAnalyzerBuilder {
	private static final String SOLR_LUCENE_VERSION_PARAM = "luceneMatchVersion";

	private SolrAnalyzerBuilder() {
	}

	/**
	 * Builds a Lucene <code>Analyzer</code> from the specified <code>AnalyzerDef</code> annotation.
	 *
	 * @param analyzerDef The <code>AnalyzerDef</code> annotation as found in the annotated domain class.
	 * @param luceneMatchVersion The lucene version (required since Lucene 3.x)
	 * @return a Lucene <code>Analyzer</code>
	 * @throws IOException
	 */
	public static Analyzer buildAnalyzer(AnalyzerDef analyzerDef,
			Version luceneMatchVersion,
			ServiceManager serviceManager) throws IOException {
		ResourceLoader defaultResourceLoader = new HibernateSearchResourceLoader( serviceManager );
		TokenizerDef token = analyzerDef.tokenizer();
		final Map<String, String> tokenMapsOfParameters = getMapOfParameters( token.params(), luceneMatchVersion );
		TokenizerFactory tokenFactory = instanceFromClass(
				TokenizerFactory.class,
				token.factory(),
				"Tokenizer factory",
				tokenMapsOfParameters
		);
		injectResourceLoader( tokenFactory, defaultResourceLoader, tokenMapsOfParameters );

		final int length = analyzerDef.filters().length;
		final int charLength = analyzerDef.charFilters().length;
		TokenFilterFactory[] filters = new TokenFilterFactory[length];
		CharFilterFactory[] charFilters = new CharFilterFactory[charLength];
		for ( int index = 0; index < length; index++ ) {
			TokenFilterDef filterDef = analyzerDef.filters()[index];
			final Map<String, String> mapOfParameters = getMapOfParameters( filterDef.params(), luceneMatchVersion );
			filters[index] = instanceFromClass(
					TokenFilterFactory.class,
					filterDef.factory(),
					"Token filter factory",
					mapOfParameters
			);
			injectResourceLoader( filters[index], defaultResourceLoader, mapOfParameters );
		}
		for ( int index = 0; index < charFilters.length; index++ ) {
			CharFilterDef charFilterDef = analyzerDef.charFilters()[index];
			final Map<String, String> mapOfParameters = getMapOfParameters(
					charFilterDef.params(),
					luceneMatchVersion
			);
			charFilters[index] = instanceFromClass(
					CharFilterFactory.class,
					charFilterDef.factory(),
					"Character filter factory",
					mapOfParameters
			);
			injectResourceLoader( charFilters[index], defaultResourceLoader, mapOfParameters );
		}
		return new TokenizerChain( charFilters, tokenFactory, filters );
	}

	private static void injectResourceLoader(Object processor, ResourceLoader defaultResourceLoader, Map<String, String> mapOfParameters) throws IOException {
		if ( processor instanceof ResourceLoaderAware ) {
			( (ResourceLoaderAware) processor ).inform( defaultResourceLoader );
		}
	}

	private static Map<String, String> getMapOfParameters(Parameter[] params, Version luceneMatchVersion) {
		Map<String, String> mapOfParams = new HashMap<String, String>( params.length );
		for ( Parameter param : params ) {
			mapOfParams.put( param.name(), param.value() );
		}
		mapOfParams.put( SOLR_LUCENE_VERSION_PARAM, luceneMatchVersion.toString() );
		return mapOfParams;
	}
}
