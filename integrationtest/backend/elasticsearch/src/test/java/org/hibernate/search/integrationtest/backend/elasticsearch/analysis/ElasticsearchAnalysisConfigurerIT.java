/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.analysis;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.assertj.core.api.Assertions;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchAnalysisConfigurerIT {

	private static final String ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX = "Error while applying analysis configuration";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void error_invalidReference() {
		Assertions.assertThatThrownBy(
				() -> setup( "foobar" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Unable to convert configuration property 'hibernate.search.backend."
										+ ElasticsearchBackendSettings.ANALYSIS_CONFIGURER + "'",
								"'foobar'",
								"Unable to find " + ElasticsearchAnalysisConfigurer.class.getName() + " implementation class: foobar"
						)
						.build()
				);
	}

	@Test
	public void error_failingConfigurer() {
		Assertions.assertThatThrownBy(
				() -> setup( FailingConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								FailingConfigurer.FAILURE_MESSAGE
						)
						.build()
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
	public void error_analyzer_namingConflict() {
		Assertions.assertThatThrownBy(
				() -> setup( AnalyzerNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Multiple analyzer definitions with the same name",
								"'analyzerName'"
						)
						.build()
				);
	}

	public static class AnalyzerNamingConflictConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.analyzer( "analyzerName" ).custom().tokenizer( "whitespace" );
			context.analyzer( "anotherAnalyzerName" ).custom().tokenizer( "whitespace" );
			context.analyzer( "analyzerName" ).type( "someType" );
		}
	}

	@Test
	public void error_normalizer_namingConflict() {
		Assertions.assertThatThrownBy(
				() -> setup( NormalizerNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Multiple normalizer definitions with the same name",
								"'normalizerName'"
						)
						.build()
				);
	}

	public static class NormalizerNamingConflictConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.normalizer( "normalizerName" ).custom();
			context.normalizer( "anotherNormalizerName" ).custom();
			context.normalizer( "normalizerName" ).custom();
		}
	}

	@Test
	public void error_tokenizer_namingConflict() {
		Assertions.assertThatThrownBy(
				() -> setup( TokenizerNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Multiple tokenizer definitions with the same name",
								"'tokenizerName'"
						)
						.build()
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
	public void error_tokenizer_missingType() {
		Assertions.assertThatThrownBy(
				() -> setup( TokenizerMissingTypeConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid tokenizer definition for name 'tokenizerName'",
								"Tokenizer definitions must at least define the tokenizer type"
						)
						.build()
				);
	}

	public static class TokenizerMissingTypeConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.tokenizer( "tokenizerName" );
		}
	}

	@Test
	public void error_charFilter_namingConflict() {
		Assertions.assertThatThrownBy(
				() -> setup( CharFilterNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Multiple char filter definitions with the same name",
								"'charFilterName'"
						)
						.build()
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
	public void error_charFilter_missingType() {
		Assertions.assertThatThrownBy(
				() -> setup( CharFilterMissingTypeConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid char filter definition for name 'charFilterName'",
								"Char filter definitions must at least define the char filter type"
						)
						.build()
				);
	}

	public static class CharFilterMissingTypeConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.charFilter( "charFilterName" );
		}
	}

	@Test
	public void error_tokenFilter_namingConflict() {
		Assertions.assertThatThrownBy(
				() -> setup( TokenFilterNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Multiple token filter definitions with the same name",
								"'tokenFilterName'"
						)
						.build()
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
	public void error_tokenFilter_missingType() {
		Assertions.assertThatThrownBy(
				() -> setup( TokenFilterMissingTypeConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid token filter definition for name 'tokenFilterName'",
								"Token filter definitions must at least define the token filter type"
						)
						.build()
				);
	}

	public static class TokenFilterMissingTypeConfigurer implements ElasticsearchAnalysisConfigurer {
		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			context.tokenFilter( "tokenFilterName" );
		}
	}

	@Test
	public void error_parameter_namingConflict() {
		Assertions.assertThatThrownBy(
				() -> setup( ParameterNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Multiple parameters with the same name",
								"'parameterName'",
								"'\"value1\"'",
								"'\"value2\"'"
						)
						.build()
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

	private void setup(String analysisConfigurer) {
		setup( analysisConfigurer, c -> { } );
	}

	private void setup(String analysisConfigurer, Consumer<IndexBindingContext> mappingContributor) {
		setupHelper.start()
				.withBackendProperty( ElasticsearchBackendSettings.ANALYSIS_CONFIGURER, analysisConfigurer )
				.withIndex( StubMappedIndex.ofAdvancedNonRetrievable( mappingContributor ) )
				.setup();
	}
}
