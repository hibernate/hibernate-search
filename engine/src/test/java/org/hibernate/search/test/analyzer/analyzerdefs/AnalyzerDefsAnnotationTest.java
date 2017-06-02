/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.analyzerdefs;

import static org.fest.assertions.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.engine.impl.AnalyzerRegistry;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Assert;
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

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

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
	public void shouldContainTheDefinedAnalyzers() throws Exception {
		ExtendedSearchIntegrator factory = sfHolder.getSearchFactory();

		Set<String> analyzerNames = new LinkedHashSet<>();
		for ( SearchIntegration integration : factory.getIntegrations().values() ) {
			AnalyzerRegistry registry = integration.getAnalyzerRegistry();
			analyzerNames.addAll( registry.getNamedAnalyzerReferences().keySet() );
		}

		/*
		 * There may be other defined analyzers, because the indexing service may have
		 * default analyzer definitions.
		 */
		assertThat( analyzerNames ).contains( "package-analyzer-1", "package-analyzer-2", "class-analyzer-1", "class-analyzer-2" );
	}

	@Test
	public void shouldNotBePossibleToHaveTwoAnalyzerDefsWithTheSameName() throws Exception {
		thrown.expect( SearchException.class );

		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithError.class );
		integratorResource.create( cfg );
	}

	private void assertThatAnalyzerExists(String analyzerName) {
		for ( SearchIntegration integration : sfHolder.getSearchFactory().getIntegrations().values() ) {
			AnalyzerRegistry registry = integration.getAnalyzerRegistry();
			AnalyzerReference analyzerReference = registry.getAnalyzerReference( analyzerName );
			if ( analyzerReference != null ) {
				if ( analyzerReference.is( LuceneAnalyzerReference.class ) ) {
					assertThat( analyzerReference.unwrap( LuceneAnalyzerReference.class ).getAnalyzer() ).isNotNull();
				}
				return;
			}
		}
		Assert.fail( "Analyzer does not exist: " + analyzerName );
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
