/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Similarity;
import org.hibernate.Session;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.impl.FullTextSessionImpl;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.junit.Test;

/**
 * Tests for {@code ClassLoaderHelper}. Verifying amongst other that it throws easy to understand exceptions.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class ClassLoaderHelperTest {

	@Test
	public void testInstanceFromName() {
		IndexManager indexManager = ClassLoaderHelper.instanceFromName(
				IndexManager.class, DirectoryBasedIndexManager.class.getName(), getClass(), "Lucene index manager"
		);
		assertNotNull( indexManager );
		assertTrue( indexManager.getClass().equals( DirectoryBasedIndexManager.class ) );

		try {
			ClassLoaderHelper.instanceFromName(
					BackendQueueProcessor.class, "HeyThisClassIsNotThere", getClass(), "backend"
			);
			fail( "was expecting a SearchException" );
		}
		catch (Exception e) {
			assertEquals( e.getClass(), SearchException.class );
			assertEquals( "Unable to find backend implementation class: HeyThisClassIsNotThere", e.getMessage() );
		}
	}

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
						"org.hibernate.search.test.util.ClassLoaderHelperTest does not implement " +
						"interface org.hibernate.search.indexes.spi.IndexManager",
				IndexManager.class, ClassLoaderHelperTest.class, "Lucene index manager"
		);
		wrappingTestFromClass(
				"org.hibernate.search.impl.FullTextSessionImpl defined for component session " +
						"is missing a no-arguments constructor",
				FullTextSession.class, FullTextSessionImpl.class, "session"
		);
		wrappingTestFromClass(
				"org.hibernate.Session defined for component session is an interface: implementation required.",
				FullTextSession.class, Session.class, "session"
		);
		wrappingTestFromClass(
				"Wrong configuration of default similarity: " +
						"class org.hibernate.search.indexes.impl.DirectoryBasedIndexManager " +
						"is not a subtype of org.apache.lucene.search.Similarity",
				Similarity.class, DirectoryBasedIndexManager.class, "default similarity"
		);
		wrappingTestFromClass(
				"Unable to instantiate default similarity class: org.apache.lucene.search.Similarity. " +
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
					"Unable to instantiate analyzer class: org.hibernate.search.test.util.BarAnalyzer. " +
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
