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
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.store.impl.FSDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Hardy Ferentschik
 */
public class FSDirectorySelectionTest extends SearchTestCase {

	public void testMMapDirectoryType() {
		SessionFactory factory = createSessionFactoryUsingDirectoryType( "mmap" );
		assertCorrectDirectoryType( factory, MMapDirectory.class.getName() );
	}

	public void testNIODirectoryType() {
		SessionFactory factory = createSessionFactoryUsingDirectoryType( "nio" );
		assertCorrectDirectoryType( factory, NIOFSDirectory.class.getName() );
	}

	public void testSimpleDirectoryType() {
		SessionFactory factory = createSessionFactoryUsingDirectoryType( "simple" );
		assertCorrectDirectoryType( factory, SimpleFSDirectory.class.getName() );
	}

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
		SearchFactoryIntegrator searchFactoryIntegrator = (SearchFactoryIntegrator) fullTextSession.getSearchFactory();
		EntityIndexBinding snowIndexBinder = searchFactoryIntegrator.getIndexBinding( SnowStorm.class );
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
