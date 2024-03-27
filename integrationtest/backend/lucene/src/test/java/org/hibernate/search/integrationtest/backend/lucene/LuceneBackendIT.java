/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.analysis.impl.HibernateSearchNormalizerWrapper;
import org.hibernate.search.backend.lucene.analysis.impl.TokenizerChain;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

class LuceneBackendIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final StubMappedIndex index = StubMappedIndex.withoutFields();

	private static LuceneBackend backend;

	@BeforeAll
	static void setup() {
		SearchIntegration integration = setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup().integration();
		backend = integration.backend().unwrap( LuceneBackend.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void analyzer() {
		assertThat( backend.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
				.isNotEmpty()
				.containsInstanceOf( StandardAnalyzer.class );
		assertThat( backend.analyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ) )
				.isNotEmpty()
				.containsInstanceOf( TokenizerChain.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void analyzer_missing() {
		assertThat( backend.analyzer( "unknown" ) ).isEmpty();
		// Normalizers are not analyzers
		assertThat( backend.analyzer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) ).isEmpty();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void normalizer() {
		assertThat( backend.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
				.isNotEmpty()
				.containsInstanceOf( HibernateSearchNormalizerWrapper.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	void normalizer_missing() {
		assertThat( backend.normalizer( "unknown" ) ).isEmpty();
		// Analyzers are not normalizers
		assertThat( backend.normalizer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).isEmpty();
	}
}
