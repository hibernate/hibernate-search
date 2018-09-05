/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.directoryProvider;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

/**
 * @author Sanne Grinovero
 */
@Category(SkipOnElasticsearch.class) // Locking parameters are specific to the Lucene backend
public class CustomLockProviderTest {

	private static final String SINGLE_INSTANCE_LOCK_FQN = "org.apache.lucene.store.SingleInstanceLockFactory$SingleInstanceLock";
	private static final String SIMPLE_LOCK_FQN = "org.apache.lucene.store.SimpleFSLockFactory$SimpleFSLock";
	private static final String NATIVE_LOCK_FQN = "org.apache.lucene.store.NativeFSLockFactory$NativeFSLock";

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
					.setProperty( "hibernate.search.default.locking_strategy", "org.hibernate.NotExistingFactory" )
				.build();
			builder.close();
			fail();
		}
		catch (SearchException e) {
			assertEquals( "Unable to find locking_strategy implementation class: org.hibernate.NotExistingFactory", e.getCause().getMessage() );
		}
	}

	@Test
	public void testUseOfNativeLockingFactory() throws IOException {
		testUseOfSelectedLockingFactory( null, NATIVE_LOCK_FQN, false );
		testUseOfSelectedLockingFactory( "native", NATIVE_LOCK_FQN, false );
	}

	@Test
	public void testUseOfSingleLockingFactory() throws IOException {
		testUseOfSelectedLockingFactory( "single", SINGLE_INSTANCE_LOCK_FQN, false );
		testUseOfSelectedLockingFactory( "single", SINGLE_INSTANCE_LOCK_FQN, true );
		//default for RAMDirectory:
		testUseOfSelectedLockingFactory( null, SINGLE_INSTANCE_LOCK_FQN, true );
	}

	@Test
	public void testUseOfSimpleLockingFactory() throws IOException {
		testUseOfSelectedLockingFactory( "simple", SIMPLE_LOCK_FQN, false );
	}

	private void testUseOfSelectedLockingFactory(String optionName, String expectedLockTypeName, boolean useRamDirectory) throws IOException {
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
			EntityIndexBinding indexBindingForEntity = integrator.getIndexBindings().get( SnowStorm.class );
			DirectoryBasedIndexManager indexManager =
					(DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagerSelector().all().iterator().next();
			DirectoryProvider<?> directoryProvider = indexManager.getDirectoryProvider();
			Directory directory = directoryProvider.getDirectory();
			try ( Lock lock = directory.obtainLock( "my-lock" ) ) {
				assertEquals( expectedLockTypeName, lock.getClass().getName() );
			}
		}
		finally {
			builder.close();
		}
		assertEquals( null, CustomLockFactoryProvider.optionValue );
	}

}
