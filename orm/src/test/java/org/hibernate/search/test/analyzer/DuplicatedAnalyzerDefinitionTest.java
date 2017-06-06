/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.analyzer;

import java.lang.annotation.ElementType;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.de.GermanStemFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for HSEARCH-569.
 *
 * @author Hardy Ferentschik
 */
public class DuplicatedAnalyzerDefinitionTest extends SearchTestBase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { };
	}

	@Test
	public void testDuplicatedAnalyzerDefinitionThrowsException() throws Exception {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Entity1.class );
		config.addAnnotatedClass( Entity2.class );
		config.setProperty( "hibernate.search.default.directory_provider", "local-heap" );
		try {
			config.buildSessionFactory();
			fail( "Session creation should have failed due to duplicate analyzer definition" );
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
		Configuration config = new Configuration();
		config.getProperties().put( Environment.MODEL_MAPPING, createSearchMapping() );
		config.setProperty( "hibernate.search.default.directory_provider", "local-heap" );
		try {
			config.buildSessionFactory();
			fail( "Session creation should have failed due to duplicate analyzer definition" );
		}
		catch (SearchException e) {
			assertEquals(
					"HSEARCH000330: Multiple analyzer definitions with the same name: 'english'.",
					e.getMessage()
			);
		}
	}

	private SearchMapping createSearchMapping() {
		SearchMapping mapping = new SearchMapping();

		mapping.analyzerDef( "english", StandardTokenizerFactory.class )
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
		return mapping;
	}
}


