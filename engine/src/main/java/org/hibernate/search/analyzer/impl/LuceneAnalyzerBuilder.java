/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.io.IOException;
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
import org.hibernate.search.cfg.spi.ParameterAnnotationsReader;
import org.hibernate.search.engine.impl.TokenizerChain;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.HibernateSearchResourceLoader;

import static org.hibernate.search.util.impl.ClassLoaderHelper.instanceFromClass;

/**
 * Instances of this class are used to build Lucene analyzers.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
final class LuceneAnalyzerBuilder {

	private static final String LUCENE_VERSION_PARAM = "luceneMatchVersion";

	private final Version luceneMatchVersion;

	private final ResourceLoader resourceLoader;

	private final Map<String, AnalyzerDef> analyzerDefinitions;

	public LuceneAnalyzerBuilder(Version luceneMatchVersion, ServiceManager serviceManager,
			Map<String, AnalyzerDef> analyzerDefinitions) {
		super();
		this.luceneMatchVersion = luceneMatchVersion;
		this.resourceLoader = new HibernateSearchResourceLoader( serviceManager );
		this.analyzerDefinitions = analyzerDefinitions;
	}

	/**
	 * Build a Lucene {@link Analyzer} for the given name.
	 *
	 * @param name The name of the definition, which should match the name defined in an {@code AnalyzerDef} annotation
	 * as found in the annotated domain class.
	 * @return a Lucene {@code Analyzer}
	 */
	public Analyzer buildAnalyzer(String name) {
		AnalyzerDef analyzerDefinition = analyzerDefinitions.get( name );
		if ( analyzerDefinition == null ) {
			throw new SearchException( "Lucene analyzer found with an unknown definition: " + name );
		}
		try {
			return buildAnalyzer( analyzerDefinition );
		}
		catch (IOException e) {
			throw new SearchException( "Could not initialize Analyzer definition " + analyzerDefinition, e );
		}
	}

	private Analyzer buildAnalyzer(AnalyzerDef analyzerDef) throws IOException {
		TokenizerDef token = analyzerDef.tokenizer();
		final Map<String, String> tokenMapsOfParameters = getMapOfParameters( token.params(), luceneMatchVersion );
		TokenizerFactory tokenFactory = instanceFromClass(
				TokenizerFactory.class,
				token.factory(),
				"Tokenizer factory",
				tokenMapsOfParameters
		);
		injectResourceLoader( tokenFactory, tokenMapsOfParameters );

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
			injectResourceLoader( filters[index], mapOfParameters );
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
			injectResourceLoader( charFilters[index], mapOfParameters );
		}
		return new TokenizerChain( charFilters, tokenFactory, filters );
	}

	private void injectResourceLoader(Object processor, Map<String, String> mapOfParameters) throws IOException {
		if ( processor instanceof ResourceLoaderAware ) {
			( (ResourceLoaderAware) processor ).inform( resourceLoader );
		}
	}

	private static Map<String, String> getMapOfParameters(Parameter[] params, Version luceneMatchVersion) {
		Map<String, String> mapOfParams = ParameterAnnotationsReader.toNewMutableMap( params );
		mapOfParams.put( LUCENE_VERSION_PARAM, luceneMatchVersion.toString() );
		return mapOfParams;
	}

}
