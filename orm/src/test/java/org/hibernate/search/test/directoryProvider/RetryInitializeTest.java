/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.directoryProvider;

import static org.hibernate.search.test.directoryProvider.FSSlaveAndMasterDPTest.masterCopy;
import static org.hibernate.search.test.directoryProvider.FSSlaveAndMasterDPTest.masterMain;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies basic behavior of FSSlaveDirectoryProvider around
 * {@link org.hibernate.search.store.impl.DirectoryProviderHelper#getRetryInitializePeriod(java.util.Properties, String)}
 * (HSEARCH-323)
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class RetryInitializeTest {

	private FullTextSessionBuilder slave;
	private FullTextSessionBuilder master;
	private File root;

	@Before
	public void setUp() throws Exception {
		root = FSSlaveAndMasterDPTest.prepareDirectories( getClass().getSimpleName() );
	}

	@After
	public void tearDown() throws Exception {
		if ( slave != null ) {
			slave.close();
		}
		if ( master != null ) {
			master.close();
		}
		FSSlaveAndMasterDPTest.cleanupDirectories( root );
	}

	@Test
	public void testStandardInitialization() {
		master = createMasterNode();
		slave = createSlaveNode( false );
	}

	@Test(expected = SearchException.class)
	public void testInitiallyFailing() {
		slave = createSlaveNode( false );
	}

	@Test
	public void testMasterDelayedInitialization() {
		slave = createSlaveNode( true );

		assertNotNull( FSSlaveDirectoryProviderTestingExtension.taskScheduled );
		Long scheduledPeriod = FSSlaveDirectoryProviderTestingExtension.taskScheduledPeriod;
		assertNotNull( scheduledPeriod );
		assertEquals( Long.valueOf( 12000L ), scheduledPeriod );

		SearchIntegrator integrator = slave.getSearchFactory().unwrap( SearchIntegrator.class );

		EntityIndexBinding snowIndexBinder = integrator.getIndexBinding( SnowStorm.class );
		IndexManager[] indexManagers = snowIndexBinder.getIndexManagers();
		assertEquals( 1, indexManagers.length );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
		FSSlaveDirectoryProviderTestingExtension dp = (FSSlaveDirectoryProviderTestingExtension) indexManager.getDirectoryProvider();
		// now as master wasn't started yet, it should return a "dummy" index a RAMDirectory
		Directory directory = dp.getDirectory();
		assertTrue( directory instanceof RAMDirectory );
		dp.triggerTimerAction();
		// still didn't start it..
		directory = dp.getDirectory();
		assertTrue( directory instanceof RAMDirectory );
		// now the master goes online, at first timer tick we'll switch to the real index
		master = createMasterNode();
		dp.triggerTimerAction();
		directory = dp.getDirectory();
		assertTrue( directory instanceof FSDirectory );
	}

	private FullTextSessionBuilder createMasterNode() {
		return new FullTextSessionBuilder()
			.addAnnotatedClass( SnowStorm.class )
			.setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + masterCopy )
			.setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + masterMain )
			.setProperty( "hibernate.search.default.directory_provider", "filesystem-master" )
			.build();
	}

	private FullTextSessionBuilder createSlaveNode(boolean enableRetryInitializePeriod) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
			.addAnnotatedClass( SnowStorm.class )
			.setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + masterCopy )
			.setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + "/slave" )
			.setProperty( "hibernate.search.default.directory_provider", FSSlaveDirectoryProviderTestingExtension.class.getName() );
		if ( enableRetryInitializePeriod ) {
			builder.setProperty( "hibernate.search.default.retry_initialize_period", "12" );
		}
		return builder.build();
	}

}
