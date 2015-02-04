/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.analyzerdef;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that the {@link org.hibernate.search.annotations.AnalyzerDef} annotation can be read by the engine
 * in all the valid locations.
 *
 * @author Davide D'Alto
 */
public class AnalyzerDefAnnotationTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class );

	@Test
	public void shouldBePossibleToAnnotatePackage() throws Exception {
		assertAnalyzerExists( "package-analyzer" );
	}

	@Test
	public void shouldBePossibleToAnnotateClass() throws Exception {
		assertAnalyzerExists( "class-analyzer" );
	}

	@Test
	public void shouldContainOnlyTheDefinedAnalyzers() throws Exception {
		Map<String, Analyzer> analyzers = ( (SearchFactoryState) sfHolder.getSearchFactory() ).getAnalyzers();
		assertThat( analyzers.keySet() ).containsOnly( "package-analyzer", "class-analyzer" );
	}

	private void assertAnalyzerExists(String analyzerName) {
		Analyzer analyzer = sfHolder.getSearchFactory().getAnalyzer( analyzerName );
		assertThat( analyzer ).isNotNull();
	}

	@Indexed
	@AnalyzerDef(
			name = "class-analyzer",
			tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
	)
	static class Sample {

		@DocumentId
		long id;

		@Field
		String description;
	}
}
