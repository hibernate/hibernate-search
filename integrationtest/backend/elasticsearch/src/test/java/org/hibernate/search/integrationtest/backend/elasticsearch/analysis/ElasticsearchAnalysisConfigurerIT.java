/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.analysis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchAnalysisConfigurerIT {

	private static final String ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX = "Unable to apply analysis configuration";

	private static final String TYPE_NAME = "mainType";
	private static final String INDEX_NAME = "mainIndex";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public final TestElasticsearchClient client = TestElasticsearchClient.create();

	@Test
	void error_invalidReference() {
		assertThatThrownBy(
				() -> setup( "foobar" )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid value for configuration property 'hibernate.search.backend."
										+ ElasticsearchIndexSettings.ANALYSIS_CONFIGURER + "': 'foobar'",
								"Unable to load class 'foobar'"
						)
				);
	}

	@Test
	void error_failingConfigurer() {
		assertThatThrownBy(
				() -> setup( FailingConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								FailingConfigurer.FAILURE_MESSAGE
						)
				);
	}

	public static class FailingConfigurer implements ElasticsearchAnalysisConfigurer {
		private static final String FAILURE_MESSAGE = "Simulated failure for " + FailingConfigurer.class.getName();

		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			throw new SimulatedFailure( FAILURE_MESSAGE );
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}

	@Test
	void error_tokenizer_namingConflict() {
		assertThatThrownBy(
				() -> setup( TokenizerNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Duplicate tokenizer definitions: 'tokenizerName'",
								"Tokenizer names must be unique"
						)
				);
	}

	public static class TokenizerNamingConflictConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.tokenizer( "tokenizerName" ).type( "someType" );
			context.tokenizer( "tokenizerName" ).type( "someType" );
		}
	}

	@Test
	void error_tokenizer_missingType() {
		assertThatThrownBy(
				() -> setup( TokenizerMissingTypeConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid tokenizer definition for name 'tokenizerName'",
								"Tokenizer definitions must at least define the tokenizer type"
						)
				);
	}

	public static class TokenizerMissingTypeConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.tokenizer( "tokenizerName" );
		}
	}

	@Test
	void error_charFilter_namingConflict() {
		assertThatThrownBy(
				() -> setup( CharFilterNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Duplicate char filter definitions: 'charFilterName'",
								"Char filter names must be unique"
						)
				);
	}

	public static class CharFilterNamingConflictConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.charFilter( "charFilterName" ).type( "someType" );
			context.charFilter( "charFilterName" ).type( "someType" );
		}
	}

	@Test
	void error_charFilter_missingType() {
		assertThatThrownBy(
				() -> setup( CharFilterMissingTypeConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid char filter definition for name 'charFilterName'",
								"Char filter definitions must at least define the char filter type"
						)
				);
	}

	public static class CharFilterMissingTypeConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.charFilter( "charFilterName" );
		}
	}

	@Test
	void error_tokenFilter_namingConflict() {
		assertThatThrownBy(
				() -> setup( TokenFilterNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Duplicate token filter definitions: 'tokenFilterName'",
								"Token filter names must be unique"
						)
				);
	}

	public static class TokenFilterNamingConflictConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.tokenFilter( "tokenFilterName" ).type( "someType" );
			context.tokenFilter( "tokenFilterName" ).type( "someType" );
		}
	}

	@Test
	void error_tokenFilter_missingType() {
		assertThatThrownBy(
				() -> setup( TokenFilterMissingTypeConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid token filter definition for name 'tokenFilterName'",
								"Token filter definitions must at least define the token filter type"
						)
				);
	}

	public static class TokenFilterMissingTypeConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.tokenFilter( "tokenFilterName" );
		}
	}

	@Test
	void error_parameter_namingConflict() {
		assertThatThrownBy(
				() -> setup( ParameterNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Ambiguous value for parameter 'parameterName'",
								"'\"value1\"'",
								"'\"value2\"'"
						)
				);
	}

	public static class ParameterNamingConflictConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.analyzer( "analyzerName" ).type( "someType" )
					.param( "parameterName", "value1" )
					.param( "anotherParameterName", "someValue" )
					.param( "parameterName", "value2" );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4594")
	void multipleConfigurers() {
		StubMappedIndex index = setup( MultipleConfigurers1.class.getName() + "," + MultipleConfigurers2.class.getName() );

		assertJsonEquals(
				"{"
						+ " 'analyzer': {"
						+ "   'analyzer1': {"
						+ "     'type': 'custom',"
						+ "     'tokenizer': 'whitespace'"
						+ "   },"
						+ "   'analyzer2': {"
						+ "     'type': 'custom',"
						+ "     'tokenizer': 'whitespace'"
						+ "   }"
						+ " }"
						+ "}",
				client.index( index.name() ).settings( "index.analysis" ).get()
		);
	}

	public static class MultipleConfigurers1 implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.analyzer( "analyzer1" ).custom()
					.tokenizer( "whitespace" );
		}
	}

	public static class MultipleConfigurers2 implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.analyzer( "analyzer2" ).custom()
					.tokenizer( "whitespace" );
		}
	}

	private StubMappedIndex setup(String analysisConfigurer) {
		return setup( analysisConfigurer, c -> {} );
	}

	private StubMappedIndex setup(String analysisConfigurer, Consumer<IndexBindingContext> mappingContributor) {
		StubMappedIndex index = StubMappedIndex.ofAdvancedNonRetrievable( mappingContributor )
				.name( INDEX_NAME ).typeName( TYPE_NAME );
		setupHelper.start()
				.expectCustomBeans()
				.withBackendProperty( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER, analysisConfigurer )
				.withIndex( index )
				.setup();
		return index;
	}
}
