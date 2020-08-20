/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.mutablefactory;

import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.engine.impl.AnalyzerRegistry;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Makes sure that Filter and Analyzer definitions survive a hot reboot
 */
@TestForIssue(jiraKey = "HSEARCH-1824")
public class DefinitionsOnHotRebootTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void notForgettingDefinedFilters() {
		SearchIntegrator sf = integratorResource.create( new SearchConfigurationForTest().addClass( A.class ) );
		int defaultFilterCount = countFilters( sf );
		sf.addClasses( B.class );
		assertEquals( defaultFilterCount + 1, countFilters( sf ) );
		assertTrue( filterExists( sf, "anyFilter" ) );
		sf.addClasses( C.class );
		assertEquals( defaultFilterCount + 2, countFilters( sf ) );
		assertTrue( filterExists( sf, "anyFilter" ) );
		assertTrue( filterExists( sf, "anotherFilter" ) );
	}

	@Test
	public void notForgettingDefinedAnalyzers() {
		SearchIntegrator sf = integratorResource.create( new SearchConfigurationForTest().addClass( A.class ) );
		int defaultAnalyzerCount = countAnalyzers( sf );
		sf.addClasses( B.class );
		assertEquals( defaultAnalyzerCount + 1, countAnalyzers( sf ) );
		assertTrue( analyzerExists( sf, "anAnalyzer" ) );
		sf.addClasses( C.class );
		assertEquals( defaultAnalyzerCount + 2, countAnalyzers( sf ) );
		assertTrue( analyzerExists( sf, "anAnalyzer" ) );
		assertTrue( analyzerExists( sf, "anotherAnalyzer" ) );
	}

	private boolean analyzerExists(SearchIntegrator sf, String analyzerName) {
		for ( SearchIntegration integration : sf.unwrap( ExtendedSearchIntegrator.class ).getIntegrations().values() ) {
			AnalyzerRegistry registry = integration.getAnalyzerRegistry();
			AnalyzerReference analyzerReference = registry.getAnalyzerReference( analyzerName );
			if ( analyzerReference != null ) {
				if ( analyzerReference.is( LuceneAnalyzerReference.class ) ) {
					return analyzerReference.unwrap( LuceneAnalyzerReference.class ).getAnalyzer() != null;
				}
				else {
					return true;
				}
			}
		}
		return false;
	}

	private int countAnalyzers(SearchIntegrator sf) {
		int count = 0;
		for ( SearchIntegration integration : sf.unwrap( ExtendedSearchIntegrator.class ).getIntegrations().values() ) {
			AnalyzerRegistry registry = integration.getAnalyzerRegistry();
			count += registry.getNamedAnalyzerReferences().size();
		}
		return count;
	}

	private boolean filterExists(SearchIntegrator sf, String filterName) {
		FilterDef filterDef = sf.unwrap( SearchFactoryState.class ).getFilterDefinitions().get( filterName );
		return filterDef != null;
	}

	private int countFilters(SearchIntegrator sf) {
		return sf.unwrap( SearchFactoryState.class ).getFilterDefinitions().size();
	}

	@Indexed
	static class A {
		@DocumentId Long id;
	}

	@Indexed
	@FullTextFilterDef( name = "anyFilter", impl = ShardSensitiveOnlyFilter.class )
	@AnalyzerDef(name = "anAnalyzer", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class))
	static class B {
		@DocumentId Long id;
	}

	@Indexed
	@FullTextFilterDef( name = "anotherFilter", impl = ShardSensitiveOnlyFilter.class )
	@AnalyzerDef(name = "anotherAnalyzer", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class))
	static class C {
		@DocumentId Long id;
	}

}
