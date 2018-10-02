/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.io.IOException;
import java.util.Map;

import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.AnalyzerDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.CharFilterDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.NormalizerDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.Parameter;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.TokenFilterDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.TokenizerDef;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.util.SearchException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.Version;

/**
 * Instances of this class are used to create Lucene analyzers.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
final class LuceneAnalyzerFactory {

	private static final String LUCENE_VERSION_PARAM = "luceneMatchVersion";

	private static final Parameter[] EMPTY_PARAMETERS = new Parameter[]{};

	private final Version luceneMatchVersion;

	private final ResourceLoader resourceLoader;

	private final LuceneAnalysisDefinitionRegistry definitionRegistry;

	public LuceneAnalyzerFactory(Version luceneMatchVersion,
			ClassResolver classResolver, ResourceResolver resourceResolver,
			LuceneAnalysisDefinitionRegistry definitionRegistry) {
		super();
		this.luceneMatchVersion = luceneMatchVersion;
		this.resourceLoader = new HibernateSearchResourceLoader( classResolver, resourceResolver );
		this.definitionRegistry = definitionRegistry;
	}

	/**
	 * Create a Lucene {@link Analyzer} for the given analyzer name.
	 *
	 * @param name The name of the definition, which should match the name defined in an {@code AnalyzerDef} annotation
	 * as found in the annotated domain class.
	 * @return a Lucene {@code Analyzer}
	 */
	public Analyzer createAnalyzer(String name) {
		AnalyzerDef analyzerDefinition = definitionRegistry.getAnalyzerDefinition( name );
		if ( analyzerDefinition == null ) {
			throw new SearchException( "Lucene analyzer found with an unknown definition: " + name );
		}
		try {
			return createAnalyzer( analyzerDefinition );
		}
		catch (IOException e) {
			throw new SearchException( "Could not initialize Analyzer definition " + analyzerDefinition, e );
		}
	}

	/**
	 * Create a Lucene {@link Analyzer} for the given normalizer name.
	 *
	 * @param name The name of the definition, which should match the name defined in a {@code NormalizerDef} annotation
	 * as found in the annotated domain class.
	 * @return a Lucene {@code Analyzer} with a {@link KeywordTokenizer}, but otherwise matching the the matching {@link NormalizerDef}.
	 */
	public Analyzer createNormalizer(String name) {
		NormalizerDef definition = definitionRegistry.getNormalizerDefinition( name );
		if ( definition == null ) {
			throw new SearchException( "Lucene normalizer found with an unknown definition: " + name );
		}
		try {
			return createNormalizer( definition );
		}
		catch (IOException e) {
			throw new SearchException( "Could not initialize normalizer definition " + definition, e );
		}
	}

	private Analyzer createAnalyzer(AnalyzerDef analyzerDef) throws IOException {
		TokenizerDef tokenizer = analyzerDef.tokenizer();
		TokenizerFactory tokenizerFactory = createAnalysisComponent(
				TokenizerFactory.class, tokenizer.factory(), tokenizer.params() );

		return createAnalyzer( tokenizerFactory, analyzerDef.charFilters(), analyzerDef.filters() );
	}

	private Analyzer createNormalizer(NormalizerDef normalizerDef) throws IOException {
		TokenizerFactory tokenizerFactory = createAnalysisComponent( TokenizerFactory.class,
				KeywordTokenizerFactory.class, EMPTY_PARAMETERS );

		Analyzer normalizer = createAnalyzer(
				tokenizerFactory, normalizerDef.charFilters(), normalizerDef.filters() );

		return new HibernateSearchNormalizerWrapper( normalizer, normalizerDef.name() );
	}

	private Analyzer createAnalyzer(TokenizerFactory tokenizerFactory,
			CharFilterDef[] charFilterDefs, TokenFilterDef[] filterDefs) throws IOException {
		final int tokenFiltersLength = filterDefs.length;
		TokenFilterFactory[] filters = new TokenFilterFactory[tokenFiltersLength];
		for ( int index = 0; index < tokenFiltersLength; index++ ) {
			TokenFilterDef filterDef = filterDefs[index];
			filters[index] = createAnalysisComponent( TokenFilterFactory.class,
					filterDef.factory(),
					filterDef.params() );
		}

		final int charFiltersLength = charFilterDefs.length;
		CharFilterFactory[] charFilters = new CharFilterFactory[charFiltersLength];
		for ( int index = 0; index < charFiltersLength; index++ ) {
			CharFilterDef charFilterDef = charFilterDefs[index];
			charFilters[index] = createAnalysisComponent( CharFilterFactory.class,
					charFilterDef.factory(), charFilterDef.params() );
		}

		return new TokenizerChain( charFilters, tokenizerFactory, filters );
	}

	private <T> T createAnalysisComponent(Class<T> expectedFactoryClass,
			Class<? extends T> factoryClass,
			Parameter[] parameters) throws IOException {
		final Map<String, String> tokenMapsOfParameters = getMapOfParameters( parameters, luceneMatchVersion );
		T tokenizerFactory = ClassLoaderHelper.instanceFromClass(
				expectedFactoryClass,
				factoryClass,
				"Tokenizer factory",
				tokenMapsOfParameters
		);
		injectResourceLoader( tokenizerFactory, tokenMapsOfParameters );
		return tokenizerFactory;
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
