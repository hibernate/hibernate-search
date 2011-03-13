/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.test.directoryProvider;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.HibernateException;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import static org.hibernate.search.test.directoryProvider.FSSlaveAndMasterDPTest.masterCopy;
import static org.hibernate.search.test.directoryProvider.FSSlaveAndMasterDPTest.masterMain;

/**
 * Verifies basic behavior of FSSlaveDirectoryProvider around
 * {@link org.hibernate.search.store.DirectoryProviderHelper#getRetryInitializePeriod(Properties, String)}
 * (HSEARCH-323)
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class RetryInitializeTest extends TestCase {
	
	private FullTextSessionBuilder slave;
	private FullTextSessionBuilder master;
	
	protected void setUp() throws Exception {
		FSSlaveAndMasterDPTest.prepareDirectories();
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		FSSlaveAndMasterDPTest.cleanupDirectories();
		if ( slave != null ) slave.close();
		if ( master != null ) master.close();
	}
	
	public void testStandardInitialization() {
		master = createMasterNode();
		slave = createSlaveNode( false );
	}
	
	public void testInitiallyFailing() {
		try {
			slave = createSlaveNode( false );
			Assert.fail( "should have failed DirectoryProvider initialization" );
		}
		catch (HibernateException he) {
			//expected: master didn't initialize the slave directory
		}
	}
	
	public void testMasterDelayedInitialization() {
		slave = createSlaveNode(true);

		assertNotNull( FSSlaveDirectoryProviderTestingExtension.taskScheduled );
		Long scheduledPeriod = FSSlaveDirectoryProviderTestingExtension.taskScheduledPeriod;
		assertNotNull( scheduledPeriod );
		assertEquals( Long.valueOf( 12000L ), scheduledPeriod );
		
		DirectoryProvider[] directoryProviders = slave.getSearchFactory().getDirectoryProviders( SnowStorm.class );
		assertEquals( 1, directoryProviders.length );
		FSSlaveDirectoryProviderTestingExtension dp = (FSSlaveDirectoryProviderTestingExtension) directoryProviders[0];
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
		File root = FSSlaveAndMasterDPTest.root;
		return new FullTextSessionBuilder()
			.addAnnotatedClass( SnowStorm.class )
			.setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + masterCopy )
			.setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + masterMain )
			.setProperty( "hibernate.search.default.directory_provider", "filesystem-master" )
			.build();
	}
	
	private FullTextSessionBuilder createSlaveNode(boolean enableRetryInitializePeriod) {
		File root = FSSlaveAndMasterDPTest.root;
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
			.addAnnotatedClass( SnowStorm.class )
			.setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + masterCopy )
			.setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + slave )
			.setProperty( "hibernate.search.default.directory_provider", FSSlaveDirectoryProviderTestingExtension.class.getName() );
		if ( enableRetryInitializePeriod ) {
			builder.setProperty( "hibernate.search.default.retry_initialize_period", "12" );
		}
		return builder.build();
	}

}
