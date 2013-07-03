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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
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
	public void testFailOnInexistentLockingFactory() {
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
			SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) ftsb.getSearchFactory();
			EntityIndexBinding indexBindingForEntity = searchFactory.getIndexBinding( SnowStorm.class );
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
