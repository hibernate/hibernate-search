/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.fileleaks;

import java.io.Serializable;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.leakdetection.FileMonitoringDirectory;
import org.hibernate.search.testsupport.leakdetection.FileMonitoringDirectoryProvider;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test for HSEARCH-1090: IndexReader leaks file handles on close
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
@Category(SkipOnElasticsearch.class) // This test is specific to Lucene
public class AllFilesClosedTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	private ExtendedSearchIntegrator searchIntegrator;

	@Test
	public void testFileHandlesReleased() {
		//We initialize the SearchFactory in the test itself as we want to test it's state *after* shutdown
		searchIntegrator = initializeSearchFactory();
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
			searchIntegrator.close();
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
		ExtendedSearchIntegrator implementor = searchIntegrator.unwrap( ExtendedSearchIntegrator.class );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) implementor.getIndexManagerHolder().getIndexManager( indexName );
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
		HSQuery hsQuery = searchIntegrator.createHSQuery( new MatchAllDocsQuery(), Book.class, Dvd.class );
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
		TransactionContextForTest tc = new TransactionContextForTest();
		searchIntegrator.getWorker().performWork( work, tc );
		tc.end();
	}

	protected ExtendedSearchIntegrator initializeSearchFactory() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.default.directory_provider", FileMonitoringDirectoryProvider.class.getName() )
			.addProperty( "hibernate.search.default.reader.strategy", "shared" )
			.addProperty( "hibernate.search.index2.reader.strategy", "not-shared" ) //close all readers closed aggressively
			.addProperty( "hibernate.search.index2.exclusive_index_use", "false" ) //close all writers closed aggressively
			.addClass( Book.class )
			.addClass( Dvd.class )
			;
		overrideProperties( cfg ); //allow extending tests with different configuration
		return integratorResource.create( cfg );
	}

	protected void overrideProperties(SearchConfigurationForTest cfg) {
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
