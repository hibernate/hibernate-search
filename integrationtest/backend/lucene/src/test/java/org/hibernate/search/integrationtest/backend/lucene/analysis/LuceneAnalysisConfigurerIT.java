/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.analysis;

import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.assertj.core.api.Assertions;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class LuceneAnalysisConfigurerIT {

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
										+ LuceneBackendSettings.ANALYSIS_CONFIGURER + "'",
								"'foobar'",
								"Unable to find " + LuceneAnalysisConfigurer.class.getName() + " implementation class: foobar"
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

	public static class FailingConfigurer implements LuceneAnalysisConfigurer {
		private static final String FAILURE_MESSAGE = "Simulated failure for " + FailingConfigurer.class.getName();
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
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

	public static class AnalyzerNamingConflictConfigurer implements LuceneAnalysisConfigurer {
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			context.analyzer( "analyzerName" ).custom();
			context.analyzer( "anotherAnalyzerName" ).custom();
			context.analyzer( "analyzerName" ).instance( new StandardAnalyzer() );
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

	public static class NormalizerNamingConflictConfigurer implements LuceneAnalysisConfigurer {
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			context.normalizer( "normalizerName" ).custom();
			context.normalizer( "anotherNormalizerName" ).custom();
			context.normalizer( "normalizerName" ).instance( new StandardAnalyzer() );
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
								"'value1'",
								"'value2'"
						)
						.build()
				);
	}

	public static class ParameterNamingConflictConfigurer implements LuceneAnalysisConfigurer {
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			context.analyzer( "analyzerName" ).custom()
					.tokenizer( WhitespaceTokenizerFactory.class )
							.param( "parameterName", "value1" )
							.param( "anotherParameterName", "someValue" )
							.param( "parameterName", "value2" );
		}
	}

	private void setup(String analysisConfigurer) {
		setup( analysisConfigurer, c -> { } );
	}

	private void setup(String analysisConfigurer, Consumer<IndexBindingContext> binder) {
		StubMappedIndex index = StubMappedIndex.ofAdvancedNonRetrievable( binder );
		setupHelper.start()
				.withBackendProperty( LuceneBackendSettings.ANALYSIS_CONFIGURER, analysisConfigurer )
				.withIndex( index )
				.setup();
	}
}
