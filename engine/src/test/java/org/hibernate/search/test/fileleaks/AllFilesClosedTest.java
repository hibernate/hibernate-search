/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.fileleaks;

import java.io.Serializable;
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.test.util.leakdetection.FileMonitoringDirectory;
import org.hibernate.search.test.util.leakdetection.FileMonitoringDirectoryProvider;
import org.junit.Test;

/**
 * Test for HSEARCH-1090: IndexReader leaks file handles on close
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class AllFilesClosedTest {

	private SearchFactoryImplementor searchFactory;

	@Test
	public void testFileHandlesReleased() {
		//We initialize the SearchFactory in the test itself as we want to test it's state *after* shutdown
		searchFactory = initializeSearchFactory();
		//extract the directories now, as they won't be available after SearchFactory#close :
		FileMonitoringDirectory directoryOne = getDirectory( "index1" );
		FileMonitoringDirectory directoryTwo = getDirectory( "index2" );
		try {
			doSomeOperations();
			assertDirectoryOpen( directoryOne );
			assertDirectoryOpen( directoryTwo );
			if ( nrtNotEnabled() ) {
				assertAllFilesClosed( directoryTwo );
			}
			// directoryOne is using resource pooling
		}
		finally {
			searchFactory.close();
		}
		assertAllFilesClosed( directoryOne );
		assertAllFilesClosed( directoryTwo );
		assertDirectoryClosed( directoryOne );
		assertDirectoryClosed( directoryTwo );
	}

	/**
	 * Override point for extending test
	 */
	protected boolean nrtNotEnabled() {
		return true;
	}

	/**
	 * Verifies all files in the Directory were closed
	 */
	private void assertAllFilesClosed(FileMonitoringDirectory directory) {
		Assert.assertTrue( "not all files were closed", directory.allFilesWereClosed() );
	}

	/**
	 * Verifies the directory is closed
	 */
	private void assertDirectoryClosed(FileMonitoringDirectory directory) {
		Assert.assertTrue( directory.isClosed() );
	}

	/**
	 * Verifies the directory is open
	 */
	private void assertDirectoryOpen(FileMonitoringDirectory directory) {
		Assert.assertFalse( directory.isClosed() );
	}

	private FileMonitoringDirectory getDirectory(String indexName) {
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) searchFactory.getIndexManagerHolder().getIndexManager( indexName );
		FileMonitoringDirectoryProvider directoryProvider = (FileMonitoringDirectoryProvider) indexManager.getDirectoryProvider();
		FileMonitoringDirectory directory = (FileMonitoringDirectory) directoryProvider.getDirectory();
		return directory;
	}

	/**
	 * The reported bugs were related to multithreaded operations, but
	 * we can actually trigger it with a sequence of read/writes: we need to re-open
	 * a dirty index to run some query on it.
	 */
	private void doSomeOperations() {
		assertElementsInIndex( 0 );
		storeDvd( 1, "Aliens" );
		storeDvd( 2, "Predators" );
		storeBook( 1, "Hibernate Search, second edition" );
		assertElementsInIndex( 3 );
		storeDvd( 2, "Prometheus" ); // This is an update
		storeBook( 2, "Prometheus and the Eagle" );
		assertElementsInIndex( 4 );
	}

	/**
	 * Run the actual query. Assert sizes to verify everything else is working.
	 * @param expected number of elements found in the index
	 */
	private void assertElementsInIndex(int expected) {
		HSQuery hsQuery = searchFactory.createHSQuery();
		hsQuery
			.luceneQuery( new MatchAllDocsQuery() )
			.targetedEntities( Arrays.asList( new Class<?>[]{ Book.class, Dvd.class } ) );
		int resultSize = hsQuery.queryResultSize();
		Assert.assertEquals( expected, resultSize );
	}

	private void storeBook(int id, String string) {
		Book book = new Book();
		book.id = id;
		book.title = string;
		storeObject( book, id );
	}

	private void storeDvd(int id, String dvdTitle) {
		Dvd dvd1 = new Dvd();
		dvd1.id = id;
		dvd1.title = dvdTitle;
		storeObject( dvd1, id );
	}

	private void storeObject(Object entity, Serializable id) {
		Work work = new Work( entity, id, WorkType.UPDATE, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}

	protected SearchFactoryImplementor initializeSearchFactory() {
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", FileMonitoringDirectoryProvider.class.getName() )
			.addProperty( "hibernate.search.default.reader.strategy", "shared" )
			.addProperty( "hibernate.search.index2.reader.strategy", "not-shared" ) //close all readers closed aggressively
			.addProperty( "hibernate.search.index2.exclusive_index_use", "false" ) //close all writers closed aggressively
			.addClass( Book.class )
			.addClass( Dvd.class )
			;
		overrideProperties( cfg ); //allow extending tests with different configuration
		return new SearchFactoryBuilder()
			.configuration( cfg )
			.buildSearchFactory();
	}

	protected void overrideProperties(ManualConfiguration cfg) {
		//nothing to do
	}

	/** Two mapped entities on two differently configured indexes **/

	@Indexed(index = "index1")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

	@Indexed(index = "index2")
	public static final class Book {
		@DocumentId long id;
		@Field String title;
	}

}
