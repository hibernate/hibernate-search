/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.analyzerdef;

import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@TestForIssue(jiraKey = "HSEARCH-2606")
public class AnalyzerDefInvalidTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

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
}
