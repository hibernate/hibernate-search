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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Similarity;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.backend.impl.batchlucene.LuceneBatchBackend;
import org.hibernate.search.impl.FullTextSessionImpl;
import org.hibernate.search.util.ClassLoaderHelper;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@code ClassLoaderHelper}. Verifying amongst other that it throws easy to understand exceptions.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class ClassLoaderHelperTest {

	@Test
	public void testInstanceFromName() {
		BatchBackend batchBackend = ClassLoaderHelper.instanceFromName(
				BatchBackend.class, LuceneBatchBackend.class.getName(), getClass(), "Lucene batch backend"
		);
		assertNotNull( batchBackend );
		assertTrue( batchBackend.getClass().equals( LuceneBatchBackend.class ) );

		try {
			ClassLoaderHelper.instanceFromName(
					BackendQueueProcessorFactory.class, "HeyThisClassIsNotThere", getClass(), "backend"
			);
			fail( "was expecting a SearchException" );
		}
		catch ( Exception e ) {
			assertEquals( e.getClass(), SearchException.class );
			assertEquals( "Unable to find backend implementation class: HeyThisClassIsNotThere", e.getMessage() );
		}
	}

	@Test
	public void testInstanceFromClass() {
		//testing for interface implementation:
		BatchBackend batchBackend = ClassLoaderHelper.instanceFromClass(
				BatchBackend.class, LuceneBatchBackend.class, "Lucene batch backend"
		);
		assertNotNull( batchBackend );
		assertTrue( batchBackend.getClass().equals( LuceneBatchBackend.class ) );

		//testing for subclasses:
		Similarity sim = ClassLoaderHelper.instanceFromClass(
				Similarity.class, DefaultSimilarity.class, "default similarity"
		);
		assertNotNull( sim );
		assertTrue( sim.getClass().equals( DefaultSimilarity.class ) );

		//testing proper error messages:
		wrappingTestFromClass(
				"Wrong configuration of Lucene batch backend: class " +
						"org.hibernate.search.test.util.ClassLoaderHelperTest does not implement " +
						"interface org.hibernate.search.backend.impl.batchlucene.BatchBackend",
				BatchBackend.class, ClassLoaderHelperTest.class, "Lucene batch backend"
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
						"class org.hibernate.search.backend.impl.batchlucene.LuceneBatchBackend " +
						"is not a subtype of org.apache.lucene.search.Similarity",
				Similarity.class, LuceneBatchBackend.class, "default similarity"
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
				StandardAnalyzer.class, org.apache.lucene.util.Version.LUCENE_30
		);
		assertNotNull( "We should be able to instantiate an analyzer with a Lucene version parameter", analyzer );
	}

	@Test
	public void testLoadingAnalyzerWithDefaultConstructor() {
		Analyzer analyzer = ClassLoaderHelper.analyzerInstanceFromClass(
				FooAnalyzer.class, org.apache.lucene.util.Version.LUCENE_30
		);
		assertNotNull( "We should be able to instantiate an analyzer which has only a default constructor", analyzer );
	}

	@Test
	public void testLoadingAnalyzerWithNoVersionOrDefaultConstructor() {
		try {
			ClassLoaderHelper.analyzerInstanceFromClass(
					BarAnalyzer.class, org.apache.lucene.util.Version.LUCENE_30
			);
			fail( "We should not be able to instantiate a analyzer with no default constructor or simple Version parameter." );
		}
		catch ( SearchException e ) {
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
		catch ( Exception e ) {
			assertEquals( e.getClass(), SearchException.class );
			assertEquals( expectedErrorMessage, e.getMessage() );
		}
	}
}

