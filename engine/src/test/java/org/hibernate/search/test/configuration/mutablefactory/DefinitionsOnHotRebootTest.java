/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.mutablefactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Makes sure that Filter and Analyzer definitions survive a hot reboot
 */
@TestForIssue(jiraKey = "HSEARCH-1824")
public class DefinitionsOnHotRebootTest {

	@Test
	public void notForgettingDefinedFilters() {
		SearchIntegratorBuilder emptySearchBuilder = new SearchIntegratorBuilder().configuration( new SearchConfigurationForTest() );
		try ( SearchIntegrator sf = emptySearchBuilder.buildSearchIntegrator() ) {
			assertEquals( 0, countFilters( sf ) );
			sf.addClasses( A.class );
			assertEquals( 1, countFilters( sf ) );
			assertTrue( filterExists( sf, "anyFilter" ) );
			sf.addClasses( B.class );
			assertEquals( 2, countFilters( sf ) );
			assertTrue( filterExists( sf, "anyFilter" ) );
			assertTrue( filterExists( sf, "anotherFilter" ) );
		}
	}

	@Test
	public void notForgettingDefinedAnalyzers() {
		SearchIntegratorBuilder emptySearchBuilder = new SearchIntegratorBuilder().configuration( new SearchConfigurationForTest() );
		try ( SearchIntegrator sf = emptySearchBuilder.buildSearchIntegrator() ) {
			assertEquals( 0, countAnalyzers( sf ) );
			sf.addClasses( A.class );
			assertEquals( 1, countAnalyzers( sf ) );
			assertTrue( analyzerExists( sf, "anAnalyzer" ) );
			sf.addClasses( B.class );
			assertEquals( 2, countAnalyzers( sf ) );
			assertTrue( analyzerExists( sf, "anAnalyzer" ) );
			assertTrue( analyzerExists( sf, "anotherAnalyzer" ) );
		}
	}

	private boolean analyzerExists(SearchIntegrator sf, String analyzerName) {
		Analyzer analyzer = sf.unwrap( SearchFactoryState.class ).getAnalyzers().get( analyzerName );
		return analyzer != null;
	}

	private int countAnalyzers(SearchIntegrator sf) {
		return sf.unwrap( SearchFactoryState.class ).getAnalyzers().size();
	}

	private boolean filterExists(SearchIntegrator sf, String filterName) {
		FilterDef filterDef = sf.unwrap( SearchFactoryState.class ).getFilterDefinitions().get( filterName );
		return filterDef != null;
	}

	private int countFilters(SearchIntegrator sf) {
		return sf.unwrap( SearchFactoryState.class ).getFilterDefinitions().size();
	}

	@Indexed
	@FullTextFilterDef( name = "anyFilter", impl = ShardSensitiveOnlyFilter.class )
	@AnalyzerDef(name = "anAnalyzer", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class))
	static class A {
		@DocumentId Long id;
	}

	@Indexed
	@FullTextFilterDef( name = "anotherFilter", impl = ShardSensitiveOnlyFilter.class )
	@AnalyzerDef(name = "anotherAnalyzer", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class))
	static class B {
		@DocumentId Long id;
	}

}
