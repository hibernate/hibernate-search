/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.directoryProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;

/**
 * @author Sanne Grinovero
 */
public class CustomLockProviderTest {

	@Test
	public void testUseOfCustomLockingFactory() {
		assertNull( CustomLockFactoryProvider.optionValue );
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		builder
			.addAnnotatedClass( SnowStorm.class )
			.setProperty( "hibernate.search.default.locking_option", "somethingHere" )
			.setProperty( "hibernate.search.default.locking_strategy",
					"org.hibernate.search.test.directoryProvider.CustomLockFactoryProvider" )
			.build();
		builder.close();
		assertEquals( "somethingHere", CustomLockFactoryProvider.optionValue );
		CustomLockFactoryProvider.optionValue = null;
	}

	@Test
	public void testFailOnNonExistentLockingFactory() {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		try {
			builder
				.addAnnotatedClass( SnowStorm.class )
				.setProperty( "hibernate.search.default.locking_option", "somethingHere" )
				.setProperty( "hibernate.search.default.locking_strategy", "org.hibernate.NotExistingFactory")
				.build();
			builder.close();
			fail();
		}
		catch (SearchException e) {
			assertEquals( "Unable to find locking_strategy implementation class: org.hibernate.NotExistingFactory", e.getCause().getMessage() );
		}
	}

	@Test
	public void testUseOfNativeLockingFactory() {
		testUseOfSelectedLockingFactory( null, NativeFSLockFactory.class, false );
		testUseOfSelectedLockingFactory( "native", NativeFSLockFactory.class, false );
	}

	@Test
	public void testUseOfSingleLockingFactory() {
		testUseOfSelectedLockingFactory( "single", SingleInstanceLockFactory.class, false );
		testUseOfSelectedLockingFactory( "single", SingleInstanceLockFactory.class, true );
		//default for RAMDirectory:
		testUseOfSelectedLockingFactory( null, SingleInstanceLockFactory.class, true );
	}

	@Test
	public void testUseOfSimpleLockingFactory() {
		testUseOfSelectedLockingFactory( "simple", SimpleFSLockFactory.class, false );
	}

	private void testUseOfSelectedLockingFactory(String optionName, Class expectedType, boolean useRamDirectory) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		FullTextSessionBuilder fullTextSessionBuilder = builder.addAnnotatedClass( SnowStorm.class );
		if ( optionName != null ) {
			fullTextSessionBuilder.setProperty( "hibernate.search.default.locking_strategy", optionName );
		}
		if ( ! useRamDirectory ) {
			fullTextSessionBuilder.useFileSystemDirectoryProvider( CustomLockProviderTest.class );
		}
		FullTextSessionBuilder ftsb = fullTextSessionBuilder.build();
		try {
			SearchIntegrator integrator = ftsb.getSearchFactory().unwrap( SearchIntegrator.class );
			EntityIndexBinding indexBindingForEntity = integrator.getIndexBinding( SnowStorm.class );
			DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagers()[0];
			DirectoryProvider directoryProvider = indexManager.getDirectoryProvider();
			Directory directory = directoryProvider.getDirectory();
			LockFactory lockFactory = directory.getLockFactory();
			assertEquals( expectedType, lockFactory.getClass() );
		}
		finally {
			builder.close();
		}
		assertEquals( null, CustomLockFactoryProvider.optionValue );
	}

}
