/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.documentation.backend.lucene.analyzer.Book;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ApplyAnalyzerNormalizerIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		DocumentationSetupHelper.SetupContext setupContext = setupHelper.start();
		if ( BackendConfiguration.isElasticsearch() ) {
			setupContext.withBackendProperty(
					ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
					"org.hibernate.search.documentation.analysis.ApplyAnalyzerNormalizerElasticsearchAnalysisConfigurer"
			);
		}
		else {
			setupContext.withBackendProperty(
					LuceneBackendSettings.ANALYSIS_CONFIGURER,
					"org.hibernate.search.documentation.analysis.ApplyAnalyzerNormalizerLuceneAnalysisConfigurer"
			);
		}
		entityManagerFactory = setupContext.setup( Book.class );
	}

	@Test
	void analyzer() {
		//tag::analyzer[]
		SearchMapping mapping = /* ... */ // <1>
				//end::analyzer[]
				Search.mapping( entityManagerFactory );
		//tag::analyzer[]
		IndexManager indexManager = mapping.indexManager( "Book" ); // <2>

		List<? extends AnalysisToken> tokens = indexManager.analyze( // <3>
				"my-analyzer", // <4>
				"The quick brown fox jumps right over the little lazy dog" // <5>
		);
		for ( AnalysisToken token : tokens ) { // <6>
			String term = token.term();
			int startOffset = token.startOffset();
			int endOffset = token.endOffset();
			// ...
		}
		//end::analyzer[]
		assertThat( tokens ).hasSize( 11 );
	}

	@Test
	void normalizer() {
		//tag::normalizer[]
		SearchMapping mapping = /* ... */ // <1>
				//end::normalizer[]
				Search.mapping( entityManagerFactory );
		//tag::normalizer[]
		IndexManager indexManager = mapping.indexManager( "Book" ); // <2>

		AnalysisToken normalizedToken = indexManager.normalize( // <3>
				"my-normalizer", // <4>
				"The quick brown fox jumps right over the little lazy dog" // <5>
		);
		String term = normalizedToken.term(); // <6>
		// ...
		//end::normalizer[]
		assertThat( term ).isEqualTo( "the quick brown fox jumps right over the little lazy dog" );
	}

}
