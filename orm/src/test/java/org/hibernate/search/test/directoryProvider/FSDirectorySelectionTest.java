/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.directoryProvider;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.impl.FSDirectoryProvider;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class FSDirectorySelectionTest extends SearchTestBase {

	@Test
	public void testMMapDirectoryType() {
		SessionFactory factory = createSessionFactoryUsingDirectoryType( "mmap" );
		assertCorrectDirectoryType( factory, MMapDirectory.class.getName() );
	}

	@Test
	public void testNIODirectoryType() {
		SessionFactory factory = createSessionFactoryUsingDirectoryType( "nio" );
		assertCorrectDirectoryType( factory, NIOFSDirectory.class.getName() );
	}

	@Test
	public void testSimpleDirectoryType() {
		SessionFactory factory = createSessionFactoryUsingDirectoryType( "simple" );
		assertCorrectDirectoryType( factory, SimpleFSDirectory.class.getName() );
	}

	@Test
	public void testInvalidDirectoryType() {
		try {
			createSessionFactoryUsingDirectoryType( "foobar" );
			fail( "Factory creation should fail with invalid 'hibernate.search.default.filesystem_access_type' parameter " );
		}
		catch (SearchException e) {
			//success
		}
	}

	private void assertCorrectDirectoryType(SessionFactory factory, String className) {
		Session session = factory.openSession();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		SearchIntegrator integrator = fullTextSession.getSearchFactory().unwrap( SearchIntegrator.class );
		EntityIndexBinding snowIndexBinder = integrator.getIndexBinding( SnowStorm.class );
		IndexManager[] indexManagers = snowIndexBinder.getIndexManagers();
		assertTrue( "Wrong number of directory providers", indexManagers.length == 1 );

		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
		Directory directory = indexManager.getDirectoryProvider().getDirectory();
		assertEquals( "Wrong directory provider type", className, directory.getClass().getName() );
		session.close();
	}

	private SessionFactory createSessionFactoryUsingDirectoryType(String directoryType) {
		Configuration config = new Configuration();
		config.addAnnotatedClass( SnowStorm.class );
		config.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
		config.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		config.setProperty( "hibernate.search.default.filesystem_access_type", directoryType );
		return config.buildSessionFactory();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { };
	}
}
