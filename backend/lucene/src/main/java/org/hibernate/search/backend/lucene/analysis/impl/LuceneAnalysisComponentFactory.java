/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.apache.lucene.util.Version;

/**
 * Instances of this class are used to create Lucene analyzers, normalizers, tokenizers, char filters and token filters.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class LuceneAnalysisComponentFactory {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String LUCENE_VERSION_PARAM = "luceneMatchVersion";

	private static final KeywordTokenizerFactory KEYWORD_TOKENIZER_FACTORY =
			new KeywordTokenizerFactory( Collections.emptyMap() );

	private final Version luceneMatchVersion;

	private final ResourceLoader resourceLoader;

	public LuceneAnalysisComponentFactory(Version luceneMatchVersion,
			ClassResolver classResolver, ResourceResolver resourceResolver) {
		super();
		this.luceneMatchVersion = luceneMatchVersion;
		this.resourceLoader = new HibernateSearchResourceLoader( classResolver, resourceResolver );
	}

	public Analyzer createAnalyzer(TokenizerFactory tokenizerFactory,
			CharFilterFactory[] charFilterFactories, TokenFilterFactory[] filterFactories) {
		return new TokenizerChain( charFilterFactories, tokenizerFactory, filterFactories );
	}

	public Analyzer createNormalizer(String name,
			CharFilterFactory[] charFilterFactories, TokenFilterFactory[] filterFactories) {
		Analyzer normalizer = new TokenizerChain( charFilterFactories, KEYWORD_TOKENIZER_FACTORY, filterFactories );
		return wrapNormalizer( name, normalizer );
	}

	public Analyzer wrapNormalizer(String name, Analyzer normalizer) {
		return new HibernateSearchNormalizerWrapper( name, normalizer );
	}

	public TokenizerFactory createTokenizerFactory(Class<? extends TokenizerFactory> factoryClass,
			Map<String, String> parameters)
			throws IOException {
		return createAnalysisComponent( TokenizerFactory.class, factoryClass, parameters );
	}

	public CharFilterFactory createCharFilterFactory(Class<? extends CharFilterFactory> factoryClass,
			Map<String, String> parameters)
			throws IOException {
		return createAnalysisComponent( CharFilterFactory.class, factoryClass, parameters );
	}

	public TokenFilterFactory createTokenFilterFactory(Class<? extends TokenFilterFactory> factoryClass,
			Map<String, String> parameters)
			throws IOException {
		return createAnalysisComponent( TokenFilterFactory.class, factoryClass, parameters );
	}

	private <T> T createAnalysisComponent(Class<T> expectedFactoryClass,
			Class<? extends T> factoryClass, Map<String, String> parameters)
			throws IOException {
		try {
			final Map<String, String> tokenMapsOfParameters = getMapOfParameters( parameters, luceneMatchVersion );
			T tokenizerFactory = ClassLoaderHelper.instanceFromClass(
					expectedFactoryClass,
					factoryClass,
					tokenMapsOfParameters
			);
			injectResourceLoader( tokenizerFactory );
			return tokenizerFactory;
		}
		catch (RuntimeException e) {
			throw log.unableToCreateAnalysisComponent( factoryClass, e.getMessage(), e );
		}
	}

	private void injectResourceLoader(Object processor) throws IOException {
		if ( processor instanceof ResourceLoaderAware ) {
			( (ResourceLoaderAware) processor ).inform( resourceLoader );
		}
	}

	private static Map<String, String> getMapOfParameters(Map<String, String> params, Version luceneMatchVersion) {
		Map<String, String> mapOfParams = new LinkedHashMap<>( params );
		params.put( LUCENE_VERSION_PARAM, luceneMatchVersion.toString() );
		return mapOfParams;
	}
}
