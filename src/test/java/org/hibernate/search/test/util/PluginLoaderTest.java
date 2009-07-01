// $Id$
package org.hibernate.search.test.util;

import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Similarity;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.backend.impl.batchlucene.LuceneBatchBackend;
import org.hibernate.search.impl.FullTextSessionImpl;
import org.hibernate.search.util.PluginLoader;

import junit.framework.TestCase;

/**
 * Test for PluginLoader, also verifying it throws easy to understand exceptions
 * 
 * @author Sanne Grinovero
 */
public class PluginLoaderTest extends TestCase {
	
	public void testInstanceFromName() {
		BatchBackend batchBackend = PluginLoader.instanceFromName(BatchBackend.class, LuceneBatchBackend.class.getName(), getClass(), "Lucene batch backend");
		assertNotNull( batchBackend );
		assertTrue( batchBackend.getClass().equals( LuceneBatchBackend.class ) );
		
		try {
			PluginLoader.instanceFromName( BackendQueueProcessorFactory.class, "HeyThisClassIsNotThere", getClass(), "backend" );
			fail( "was expecting a SearchException" );
		}
		catch (Exception e) {
			assertEquals( e.getClass(), SearchException.class );
			assertEquals( "Unable to find backend implementation class: HeyThisClassIsNotThere", e.getMessage() );
		}
	}

	public void testInstanceFromClass() {
		//testing for interface implementation:
		BatchBackend batchBackend = PluginLoader.instanceFromClass( BatchBackend.class, LuceneBatchBackend.class, "Lucene batch backend" );
		assertNotNull( batchBackend );
		assertTrue( batchBackend.getClass().equals( LuceneBatchBackend.class ) );
		
		//testing for subclasses:
		Similarity sim =  PluginLoader.instanceFromClass( Similarity.class, DefaultSimilarity.class, "default similarity" );
		assertNotNull( sim );
		assertTrue( sim.getClass().equals( DefaultSimilarity.class ) );
		
		//testing proper error messages:
		wrappingTestFromClass(
				"Wrong configuration of Lucene batch backend: class " +
				"org.hibernate.search.test.util.PluginLoaderTest does not implement " + 
				"interface org.hibernate.search.backend.impl.batchlucene.BatchBackend",
				BatchBackend.class, PluginLoaderTest.class, "Lucene batch backend"
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
	
	private void wrappingTestFromClass(String expectedErrorMessage, Class<?> interf, Class<?> impl, String componentName) {
		try {
			PluginLoader.instanceFromClass( interf, impl, componentName );
			fail( "was expecting a SearchException" );
		}
		catch (Exception e) {
			assertEquals( e.getClass(), SearchException.class );
			assertEquals( expectedErrorMessage, e.getMessage() );
		}
	}
	
}

