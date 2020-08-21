/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.analyzerdef;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@TestForIssue(jiraKey = "HSEARCH-2606")
public class AnalyzerDefInvalidTest {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	private SearchIntegrator integrator;

	private final SearchITHelper helper = new SearchITHelper( () -> this.integrator );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void shouldNotBePossibleToHaveTwoAnalyzerParametersWithTheSameName() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Conflicting usage of @Parameter annotation for parameter name: 'maxGramSize'. Can't assign both value '15' and '1'" );

		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithAnalyzer.class );
		integratorResource.create( cfg );
	}

	@Test
	public void shouldNotBePossibleToHaveTwoNormalizerParametersWithTheSameName() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Conflicting usage of @Parameter annotation for parameter name: 'pattern'. Can't assign both value '[[:digit:]]' and '[[:digit:]]+'" );

		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithNormalizer.class );
		integratorResource.create( cfg );
	}

	@Test
	public void shouldNotBePossibleToHaveEmptyNormalizer() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000343" );
		thrown.expectMessage( "'empty_normalizer'" );
		thrown.expectMessage( "must define at least a char filter or a token filter" );

		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithEmptyNormalizer.class );
		integratorResource.create( cfg );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // This test only works with locally-executed normalizers
	public void shouldWarnOnTokenizingNormalizerDefinition() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithTokenizingNormalizerDefinition.class );
		integrator = integratorResource.create( cfg );

		logged.expectMessage( "HSEARCH000344", "'tokenizing_normalizer'", "3 tokens" );

		SampleWithTokenizingNormalizerDefinition entity = new SampleWithTokenizingNormalizerDefinition();
		entity.id = 1;
		entity.description = "to be tokenized";
		helper.add( entity, entity.id );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // This test only works with locally-executed normalizers
	public void shouldWarnOnTokenizingNormalizerImplementation() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithTokenizingNormalizerImplementation.class );
		integrator = integratorResource.create( cfg );

		logged.expectMessage( "HSEARCH000344", "'" + StandardAnalyzer.class.getName() + "'", "2 tokens" );

		SampleWithTokenizingNormalizerImplementation entity = new SampleWithTokenizingNormalizerImplementation();
		entity.id = 1;
		entity.description = "a description to be tokenized";
		helper.add( entity, entity.id );
	}

	@Test
	public void shouldWarnOnSortableFieldWithNonNormalizerAnalyzer() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithTokenizedSortableField.class );

		logged.expectMessage( "HSEARCH000345", "'" + SampleWithTokenizedSortableField.class.getName() + "'",
				"'sortableField'", "Sortable fields should be assigned normalizers" );

		integratorResource.create( cfg );
	}

	@Indexed
	@AnalyzerDef(name = "ngram", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), filters = {
			@TokenFilterDef(factory = EdgeNGramFilterFactory.class, params = {
					@Parameter(name = "maxGramSize", value = "1"),
					@Parameter(name = "maxGramSize", value = "15") // Illegal: mentioned the same Parameter name again
			})
	})
	static class SampleWithAnalyzer {

		@DocumentId
		long id;

		@Field(analyzer = @Analyzer(definition = "ngram"))
		String description;
	}

	@Indexed
	@NormalizerDef(name = "ngram", charFilters = {
			@CharFilterDef(factory = PatternReplaceCharFilterFactory.class, params = {
					@Parameter(name = "pattern", value = "[[:digit:]]+"),
					@Parameter(name = "pattern", value = "[[:digit:]]"), // Illegal: mentioned the same Parameter name again
					@Parameter(name = "replacement", value = "0")
			})
	})
	static class SampleWithNormalizer {

		@DocumentId
		long id;

		@Field(normalizer = @Normalizer(definition = "ngram"))
		String description;
	}

	@Indexed
	@NormalizerDef(name = "empty_normalizer")
	static class SampleWithEmptyNormalizer {

		@DocumentId
		long id;

		@Field(normalizer = @Normalizer(definition = "empty_normalizer"))
		String description;
	}

	@Indexed
	@NormalizerDef(name = "tokenizing_normalizer", filters = @TokenFilterDef(factory = CustomFilterFactory.class))
	static class SampleWithTokenizingNormalizerDefinition {

		@DocumentId
		long id;

		@Field(normalizer = @Normalizer(definition = "tokenizing_normalizer"))
		String description;
	}

	@Indexed
	static class SampleWithTokenizingNormalizerImplementation {

		@DocumentId
		long id;

		@Field(normalizer = @Normalizer(impl = StandardAnalyzer.class))
		String description;
	}

	@Indexed
	@AnalyzerDef(name = "tokenizing_analyzer", tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class))
	static class SampleWithTokenizedSortableField {

		@DocumentId
		long id;

		@Field(analyzer = @Analyzer(definition = "tokenizing_analyzer"))
		@SortableField
		String sortableField;
	}

	public static final class CustomFilterFactory extends TokenFilterFactory {

		public CustomFilterFactory(Map<String, String> args) {
			super( args );
		}

		@Override
		public TokenStream create(TokenStream input) {
			return new TokenFilter( input ) {
				private final Deque<String> nextTokens = new ArrayDeque<>();
				private final CharTermAttribute termAtt = addAttribute( CharTermAttribute.class );
				@Override
				public boolean incrementToken() throws IOException {
					if ( !nextTokens.isEmpty() ) {
						termAtt.setEmpty().append( nextTokens.removeFirst() );
						return true;
					}
					if ( input.incrementToken() ) {
						for ( String token : termAtt.toString().split( " " ) ) {
							nextTokens.addLast( token );
						}
						if ( !nextTokens.isEmpty() ) {
							termAtt.setEmpty().append( nextTokens.removeFirst() );
							return true;
						}
					}
					return false;
				}
			};
		}

	}
}
