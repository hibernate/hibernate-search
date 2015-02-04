/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.testsupport.analyzer.BarAnalyzer;
import org.hibernate.search.testsupport.analyzer.FooAnalyzer;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@code ClassLoaderHelper}. Verifying amongst other that it throws easy to understand exceptions.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class ClassLoaderHelperTest {

	@Test
	public void testInstanceFromClass() {
		//testing for interface implementation:
		IndexManager batchBackend = ClassLoaderHelper.instanceFromClass(
				IndexManager.class, DirectoryBasedIndexManager.class, "Lucene index manager"
		);
		assertNotNull( batchBackend );
		assertTrue( batchBackend.getClass().equals( DirectoryBasedIndexManager.class ) );

		//testing for subclasses:
		Similarity sim = ClassLoaderHelper.instanceFromClass(
				Similarity.class, DefaultSimilarity.class, "default similarity"
		);
		assertNotNull( sim );
		assertTrue( sim.getClass().equals( DefaultSimilarity.class ) );

		//testing proper error messages:
		wrappingTestFromClass(
				"Wrong configuration of Lucene index manager: class " +
						"org.hibernate.search.test.util.impl.ClassLoaderHelperTest does not implement " +
						"interface org.hibernate.search.indexes.spi.IndexManager",
				IndexManager.class, ClassLoaderHelperTest.class, "Lucene index manager"
		);
		wrappingTestFromClass(
				"Wrong configuration of default similarity: " +
						"class org.hibernate.search.indexes.spi.DirectoryBasedIndexManager " +
						"is not a subtype of org.apache.lucene.search.similarities.Similarity",
				Similarity.class, DirectoryBasedIndexManager.class, "default similarity"
		);
		wrappingTestFromClass(
				"Unable to instantiate default similarity class: org.apache.lucene.search.similarities.Similarity. " +
						"Verify it has a no-args public constructor and is not abstract.",
				Similarity.class, Similarity.class, "default similarity"
		);
	}

	@Test
	public void testLoadingAnalyzerWithVersionConstructor() {
		Analyzer analyzer = ClassLoaderHelper.analyzerInstanceFromClass(
				StandardAnalyzer.class, Environment.DEFAULT_LUCENE_MATCH_VERSION
		);
		assertNotNull( "We should be able to instantiate an analyzer with a Lucene version parameter", analyzer );
	}

	@Test
	public void testLoadingAnalyzerWithDefaultConstructor() {
		Analyzer analyzer = ClassLoaderHelper.analyzerInstanceFromClass(
				FooAnalyzer.class, Environment.DEFAULT_LUCENE_MATCH_VERSION
		);
		assertNotNull( "We should be able to instantiate an analyzer which has only a default constructor", analyzer );
	}

	@Test
	public void testLoadingAnalyzerWithNoVersionOrDefaultConstructor() {
		try {
			ClassLoaderHelper.analyzerInstanceFromClass(
					BarAnalyzer.class, Environment.DEFAULT_LUCENE_MATCH_VERSION
			);
			fail( "We should not be able to instantiate a analyzer with no default constructor or simple Version parameter." );
		}
		catch (SearchException e) {
			assertEquals( e.getClass(), SearchException.class );
			assertEquals(
					"Unable to instantiate analyzer class: org.hibernate.search.testsupport.analyzer.BarAnalyzer. " +
							"Class neither has a default constructor nor a constructor with a Version parameter",
					e.getMessage()
			);
		}
	}

	private void wrappingTestFromClass(String expectedErrorMessage, Class<?> interf, Class<?> impl, String componentName) {
		try {
			ClassLoaderHelper.instanceFromClass( interf, impl, componentName );
			fail( "was expecting a SearchException" );
		}
		catch (Exception e) {
			assertEquals( e.getClass(), SearchException.class );
			assertEquals( expectedErrorMessage, e.getMessage() );
		}
	}
}
