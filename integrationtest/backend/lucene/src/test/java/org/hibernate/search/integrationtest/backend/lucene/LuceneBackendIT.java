/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.analysis.impl.HibernateSearchNormalizerWrapper;
import org.hibernate.search.backend.lucene.analysis.impl.TokenizerChain;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class LuceneBackendIT {

	private static final String BACKEND_NAME = "MyBackend";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final StubMappedIndex index = StubMappedIndex.withoutFields( "MainIndex" );

	private static LuceneBackend backend;

	@BeforeClass
	public static void setup() {
		SearchIntegration integration = setupHelper.start( BACKEND_NAME ).withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
		backend = integration.getBackend( BACKEND_NAME ).unwrap( LuceneBackend.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void analyzer() {
		assertThat( backend.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
				.isNotEmpty()
				.containsInstanceOf( StandardAnalyzer.class );
		assertThat( backend.analyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ) )
				.isNotEmpty()
				.containsInstanceOf( TokenizerChain.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void analyzer_missing() {
		assertThat( backend.analyzer( "unknown" ) ).isEmpty();
		// Normalizers are not analyzers
		assertThat( backend.analyzer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) ).isEmpty();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void normalizer() {
		assertThat( backend.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
				.isNotEmpty()
				.containsInstanceOf( HibernateSearchNormalizerWrapper.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void normalizer_missing() {
		assertThat( backend.normalizer( "unknown" ) ).isEmpty();
		// Analyzers are not normalizers
		assertThat( backend.normalizer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).isEmpty();
	}
}
