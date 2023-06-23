/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneAnalysisUtils;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternTokenizerFactory;

public class LuceneAnalysisConfigurerIT {

	private static final String ANALYSIS_CONFIGURER_ERROR_MESSAGE_PREFIX = "Unable to apply analysis configuration";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4404")
	public void availableComponents() {
		assertThat( CollectingConfigurer.TOKENIZERS ).isEmpty();
		assertThat( CollectingConfigurer.CHAR_FILTERS ).isEmpty();
		assertThat( CollectingConfigurer.TOKEN_FILTERS ).isEmpty();

		setup( CollectingConfigurer.class.getName() );

		assertThat( CollectingConfigurer.TOKENIZERS ).contains( "whitespace", "standard", "pattern" );
		assertThat( CollectingConfigurer.CHAR_FILTERS ).contains( "htmlStrip", "patternReplace" );
		assertThat( CollectingConfigurer.TOKEN_FILTERS ).contains( "lowercase", "asciiFolding" );
	}

	public static class CollectingConfigurer implements LuceneAnalysisConfigurer {
		private static final Set<String> TOKENIZERS = new HashSet<>();
		private static final Set<String> CHAR_FILTERS = new HashSet<>();
		private static final Set<String> TOKEN_FILTERS = new HashSet<>();

		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			TOKENIZERS.addAll( context.availableTokenizers() );
			CHAR_FILTERS.addAll( context.availableCharFilters() );
			TOKEN_FILTERS.addAll( context.availableTokenFilters() );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4404")
	public void byName() throws IOException {
		LuceneBackend backend = setup( ByNameConfigurer.class.getName() );

		Optional<? extends Analyzer> analyzer = backend.analyzer( "analyzer" );
		assertThat( analyzer ).isPresent();
		assertThat( LuceneAnalysisUtils.analyze( analyzer.get(), "foo", "ThIS-<strong>is</strong>-a string" ) )
				.containsExactly( "this", "is", "a string" );

		Optional<? extends Analyzer> normalizer = backend.normalizer( "normalizer" );
		assertThat( normalizer ).isPresent();
		assertThat( LuceneAnalysisUtils.analyze( normalizer.get(), "foo", "ThIS-<strong>is</strong>-a string" ) )
				.containsExactly( "this-is-a string" );
	}

	public static class ByNameConfigurer implements LuceneAnalysisConfigurer {
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			context.analyzer( "analyzer" ).custom()
					.tokenizer( "pattern" )
					.param( "pattern", "-" )
					.charFilter( "htmlStrip" )
					.tokenFilter( "lowercase" );
			context.normalizer( "normalizer" ).custom()
					.charFilter( "htmlStrip" )
					.tokenFilter( "lowercase" );
		}
	}

	@Test
	public void byClass() throws IOException {
		LuceneBackend backend = setup( ByClassConfigurer.class.getName() );

		Optional<? extends Analyzer> analyzer = backend.analyzer( "analyzer" );
		assertThat( analyzer ).isPresent();
		assertThat( LuceneAnalysisUtils.analyze( analyzer.get(), "foo", "ThIS-<strong>is</strong>-a string" ) )
				.containsExactly( "this", "is", "a string" );

		Optional<? extends Analyzer> normalizer = backend.normalizer( "normalizer" );
		assertThat( normalizer ).isPresent();
		assertThat( LuceneAnalysisUtils.analyze( normalizer.get(), "foo", "ThIS-<strong>is</strong>-a string" ) )
				.containsExactly( "this-is-a string" );
	}

	public static class ByClassConfigurer implements LuceneAnalysisConfigurer {
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			context.analyzer( "analyzer" ).custom()
					.tokenizer( PatternTokenizerFactory.class )
					.param( "pattern", "-" )
					.charFilter( HTMLStripCharFilterFactory.class )
					.tokenFilter( LowerCaseFilterFactory.class );
			context.normalizer( "normalizer" ).custom()
					.charFilter( HTMLStripCharFilterFactory.class )
					.tokenFilter( LowerCaseFilterFactory.class );
		}
	}

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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4594")
	public void multipleConfigurers() {
		LuceneBackend backend = setup( MultipleConfigurers1.class.getName() + "," + MultipleConfigurers2.class.getName() );

		assertThat( backend.analyzer( "analyzer1" ) ).isPresent();
		assertThat( backend.analyzer( "analyzer2" ) ).isPresent();
	}

	public static class MultipleConfigurers1 implements LuceneAnalysisConfigurer {
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			context.analyzer( "analyzer1" ).custom()
					.tokenizer( "whitespace" );
		}
	}

	public static class MultipleConfigurers2 implements LuceneAnalysisConfigurer {
		@Override
		public void configure(LuceneAnalysisConfigurationContext context) {
			context.analyzer( "analyzer2" ).custom()
					.tokenizer( "whitespace" );
		}
	}

	private LuceneBackend setup(String analysisConfigurer) {
		return setupHelper.start()
				.expectCustomBeans()
				.withBackendProperty( LuceneBackendSettings.ANALYSIS_CONFIGURER, analysisConfigurer )
				.withIndex( StubMappedIndex.withoutFields() )
				.setup()
				.integration()
				.backend()
				.unwrap( LuceneBackend.class );
	}
}
