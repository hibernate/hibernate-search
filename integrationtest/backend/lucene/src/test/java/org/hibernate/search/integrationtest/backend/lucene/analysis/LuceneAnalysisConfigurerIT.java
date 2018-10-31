/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.analysis;

import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.lucene.cfg.SearchBackendLuceneSettings;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class LuceneAnalysisConfigurerIT {

	private static final String MAPPED_TYPE_NAME = "TypeName";
	private static final String BACKEND_NAME = "BackendName";
	private static final String INDEX_NAME = "IndexName";

	private static final String ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX = "Error while applying analysis configuration";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void error_invalidReference() {
		SubTest.expectException(
				() -> setup( "foobar" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( MAPPED_TYPE_NAME )
						.indexContext( INDEX_NAME )
						.backendContext( BACKEND_NAME )
						.failure(
								ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX,
								// TODO HSEARCH-3388 expect the full property key instead of just the radical
								"Unable to convert configuration property 'backend." + BACKEND_NAME + "."
										+ SearchBackendLuceneSettings.ANALYSIS_CONFIGURER + "'",
								"'foobar'"
						)
						.build()
				);
	}

	@Test
	public void error_failingConfigurer() {
		SubTest.expectException(
				() -> setup( FailingConfigurer.class.getName() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( MAPPED_TYPE_NAME )
						.indexContext( INDEX_NAME )
						.backendContext( BACKEND_NAME )
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
		public void configure(LuceneAnalysisDefinitionContainerContext context) {
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
		SubTest.expectException(
				() -> setup( AnalyzerNamingConflictConfigurer.class.getName() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( MAPPED_TYPE_NAME )
						.indexContext( INDEX_NAME )
						.backendContext( BACKEND_NAME )
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
		public void configure(LuceneAnalysisDefinitionContainerContext context) {
			context.analyzer( "analyzerName" ).custom();
			context.analyzer( "anotherAnalyzerName" ).custom();
			context.analyzer( "analyzerName" ).instance( new StandardAnalyzer() );
		}
	}

	@Test
	public void error_normalizer_namingConflict() {
		SubTest.expectException(
				() -> setup( NormalizerNamingConflictConfigurer.class.getName() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( MAPPED_TYPE_NAME )
						.indexContext( INDEX_NAME )
						.backendContext( BACKEND_NAME )
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
		public void configure(LuceneAnalysisDefinitionContainerContext context) {
			context.normalizer( "normalizerName" ).custom();
			context.normalizer( "anotherNormalizerName" ).custom();
			context.normalizer( "normalizerName" ).instance( new StandardAnalyzer() );
		}
	}

	@Test
	public void error_parameter_namingConflict() {
		SubTest.expectException(
				() -> setup( ParameterNamingConflictConfigurer.class.getName() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( MAPPED_TYPE_NAME )
						.indexContext( INDEX_NAME )
						.backendContext( BACKEND_NAME )
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
		public void configure(LuceneAnalysisDefinitionContainerContext context) {
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

	private void setup(String analysisConfigurer, Consumer<IndexModelBindingContext> mappingContributor) {
		setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withProperty(
						"backend." + BACKEND_NAME + "." + SearchBackendLuceneSettings.ANALYSIS_CONFIGURER,
						analysisConfigurer
				)
				.withIndex(
						MAPPED_TYPE_NAME, INDEX_NAME,
						mappingContributor,
						indexManager -> { }
				)
				.setup();
	}
}
