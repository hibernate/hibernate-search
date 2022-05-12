/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.analysis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;

public class LuceneAnalysisConfigurerIT {

	private static final String ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX = "Unable to apply analysis configuration";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void error_invalidReference() {
		assertThatThrownBy(
				() -> setup( "foobar" )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Invalid value for configuration property 'hibernate.search.backend."
										+ LuceneBackendSettings.ANALYSIS_CONFIGURER + "': 'foobar'",
								"Unable to load class 'foobar'"
						)
				);
	}

	@Test
	public void error_failingConfigurer() {
		assertThatThrownBy(
				() -> setup( FailingConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								FailingConfigurer.FAILURE_MESSAGE
						)
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
	public void error_parameter_namingConflict() {
		assertThatThrownBy(
				() -> setup( ParameterNamingConflictConfigurer.class.getName() )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								"Ambiguous value for parameter 'parameterName'",
								"'value1'",
								"'value2'"
						)
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
				.expectCustomBeans()
				.withBackendProperty( LuceneBackendSettings.ANALYSIS_CONFIGURER, analysisConfigurer )
				.withIndex( index )
				.setup();
	}
}
