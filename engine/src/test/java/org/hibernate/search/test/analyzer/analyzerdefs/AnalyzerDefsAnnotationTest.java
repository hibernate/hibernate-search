/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.analyzerdefs;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test that the {@link org.hibernate.search.annotations.AnalyzerDefs} annotation can be read by the engine
 * in all the valid locations.
 *
 * @author Davide D'Alto
 */
public class AnalyzerDefsAnnotationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class );

	@Test
	public void shouldBePossibleToAnnotatePackage() throws Exception {
		assertThatAnalyzerExists( "package-analyzer-1" );
		assertThatAnalyzerExists( "package-analyzer-2" );
	}

	@Test
	public void shouldBePossibleToAnnotateClass() throws Exception {
		assertThatAnalyzerExists( "class-analyzer-1" );
		assertThatAnalyzerExists( "class-analyzer-2" );
	}

	@Test
	public void shouldContainOnlyTheDefinedAnalyzers() throws Exception {
		Map<String, Analyzer> analyzers = ( (SearchFactoryState) sfHolder.getSearchFactory() ).getAnalyzers();
		assertThat( analyzers.keySet() ).containsOnly( "package-analyzer-1", "package-analyzer-2", "class-analyzer-1", "class-analyzer-2" );
	}

	@Test
	public void shouldNotBePossibleToHaveTwoAnalyzerDefsWithTheSameName() throws Exception {
		thrown.expect( SearchException.class );

		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithError.class );
		new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator().close();
	}

	private void assertThatAnalyzerExists(String analyzerName) {
		Analyzer analyzer = sfHolder.getSearchFactory().getAnalyzer( analyzerName );
		assertThat( analyzer ).isNotNull();
	}

	@Indexed
	@AnalyzerDefs({
		@AnalyzerDef(
			name = "class-analyzer-1",
			tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
		),
		@AnalyzerDef(
			name = "class-analyzer-2",
			tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
		)
	})
	static class Sample {

		@DocumentId
		long id;

		@Field
		String description;
	}

	@Indexed
	@AnalyzerDefs({
		@AnalyzerDef(
			name = "package-analyzer-1",
			tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
		),
		@AnalyzerDef(
			name = "class-analyzer-unique",
			tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
		)
	})
	static class SampleWithError {

		@DocumentId
		final long id = 1;

		@Field
		final String description = "";
	}
}
