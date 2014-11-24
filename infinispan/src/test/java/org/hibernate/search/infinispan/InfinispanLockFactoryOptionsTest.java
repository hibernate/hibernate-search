/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.infinispan;

import junit.framework.Assert;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.infinispan.impl.InfinispanDirectoryProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.directoryProvider.CustomLockFactoryProvider;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.test.util.TestForIssue;
import org.infinispan.lucene.locking.BaseLockFactory;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verifies the locking_strategy option is being applied as expected, even if the
 * DirectoryProvider is set to Infinispan.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1734")
public class InfinispanLockFactoryOptionsTest {

	@Rule
	public SearchFactoryHolder holder = new SearchFactoryHolder(
					BookTypeZero.class, BookTypeOne.class, BookTypeTwo.class, BookTypeThree.class, BookTypeFour.class )
			.withProperty( "hibernate.search.default.directory_provider", "infinispan" )
			.withProperty( "hibernate.search.infinispan.configuration_resourcename", "notclustered-infinispan.xml" )
			.withProperty( "hibernate.search.INDEX1.locking_strategy", "none" )
			.withProperty( "hibernate.search.INDEX2.locking_strategy", CustomLockFactoryProvider.class.getName() )
			.withProperty( "hibernate.search.INDEX3.locking_strategy", "single" );

	@Test
	public void verifyDefaulInfinispanLock() {
		verifyLockFactoryForIndexIs( "INDEX0", BaseLockFactory.class );
	}

	@Test
	public void verifyNoLocking() {
		verifyLockFactoryForIndexIs( "INDEX1", NoLockFactory.class );
	}

	@Test
	public void verifyCustomLocking() {
		verifyLockFactoryForIndexIs( "INDEX2", SingleInstanceLockFactory.class ); //as built by the CustomLockFactoryProvider
	}

	@Test
	public void verifyExplicitSingle() {
		verifyLockFactoryForIndexIs( "INDEX3", SingleInstanceLockFactory.class );
	}

	@Test
	public void verifyDefaultIsInherited() {
		verifyLockFactoryForIndexIs( "INDEX4", BaseLockFactory.class );
	}

	private void verifyLockFactoryForIndexIs(String indexName, Class<? extends LockFactory> expectedType) {
		Directory directory = directoryByName( indexName );
		LockFactory lockFactory = directory.getLockFactory();
		Assert.assertEquals( expectedType, lockFactory.getClass() );
	}

	private Directory directoryByName(String indexName) {
		IndexManager indexManager = holder.getSearchFactory()
				.getIndexManagerHolder()
				.getIndexManager( indexName );
		Assert.assertNotNull( indexManager );
		DirectoryBasedIndexManager dpIm = (DirectoryBasedIndexManager) indexManager;
		DirectoryProvider directoryProvider = dpIm.getDirectoryProvider();
		Assert.assertNotNull( directoryProvider );
		Assert.assertTrue( "Isn't an Infinispan Directory!", directoryProvider instanceof InfinispanDirectoryProvider );
		return dpIm.getDirectoryProvider().getDirectory();
	}

	@Indexed(index = "INDEX0")
	public static class BookTypeZero {
		@DocumentId int id;
		@Field String title;
	}

	@Indexed(index = "INDEX1")
	public static class BookTypeOne extends BookTypeZero {
	}

	@Indexed(index = "INDEX2")
	public static class BookTypeTwo extends BookTypeZero {
	}

	@Indexed(index = "INDEX3")
	public static class BookTypeThree extends BookTypeZero {
	}

	@Indexed(index = "INDEX4")
	public static class BookTypeFour extends BookTypeZero {
	}

}
