/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.analyzer.common;

import java.lang.annotation.ElementType;

import org.hibernate.search.annotations.Factory;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.de.GermanStemFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for HSEARCH-569.
 *
 * @author Hardy Ferentschik
 */
public class DuplicatedAnalyzerDefinitionTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void testDuplicatedAnalyzerDefinitionThrowsException() throws Exception {
		SearchConfigurationForTest config = new SearchConfigurationForTest()
				.addClasses( Entity1.class, Entity2.class )
				.addProperty( "hibernate.search.default.directory_provider", "local-heap" );
		try {
			integratorResource.create( config );
			fail( "Integrator creation should have failed due to duplicate analyzer definition" );
		}
		catch (SearchException e) {
			assertEquals(
					"HSEARCH000330: Multiple analyzer definitions with the same name: 'my-analyzer'.",
					e.getMessage()
			);
		}
	}

	@Test
	public void testDuplicatedProgrammaticAnalyzerDefinitionThrowsException() throws Exception {
		SearchConfigurationForTest config = new SearchConfigurationForTest()
				.addProperty( Environment.MODEL_MAPPING, ProgrammaticMappingWithDuplicateAnalyzerDefinitions.class.getName() )
				.addProperty( "hibernate.search.default.directory_provider", "local-heap" );
		try {
			integratorResource.create( config );
			fail( "Integrator creation should have failed due to duplicate analyzer definition" );
		}
		catch (SearchException e) {
			assertEquals(
					"HSEARCH000330: Multiple analyzer definitions with the same name: 'english'.",
					e.getMessage()
			);
		}
	}

	@Test
	public void testDuplicatedAnalyzerDiscriminatorDefinitions() throws Exception {
		SearchConfigurationForTest config = new SearchConfigurationForTest()
				.addClass( BlogEntry.class )
				.addProperty( "hibernate.search.default.directory_provider", "local-heap" );
		try {
			integratorResource.create( config );
			fail( "Integrator creation should have failed due to duplicate analyzer definition" );
		}
		catch (SearchException e) {
			assertTrue(
					"Wrong error message",
					e.getMessage().startsWith( "Multiple AnalyzerDiscriminator defined in the same class hierarchy" )
			);
		}
	}

	public static class ProgrammaticMappingWithDuplicateAnalyzerDefinitions {
		@Factory
		public SearchMapping create() {
			SearchMapping searchMapping = new SearchMapping();
			searchMapping.analyzerDef( "english", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( SnowballPorterFilterFactory.class )
					.analyzerDef(
							"english", StandardTokenizerFactory.class
					) // ups duplicate name here - this should throw an exception
					.filter( LowerCaseFilterFactory.class )
					.filter( GermanStemFilterFactory.class )
					.entity( BlogEntry.class )
					.indexed()
					.property( "title", ElementType.METHOD );
			return searchMapping;
		}
	}
}


