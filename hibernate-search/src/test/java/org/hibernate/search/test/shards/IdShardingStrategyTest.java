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
package org.hibernate.search.test.shards;

import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IdHashShardingStrategy;
import org.hibernate.search.store.RAMDirectoryProvider;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Sanne Grinovero
 */
public class IdShardingStrategyTest {

	private IdHashShardingStrategy shardStrategy;

	@Before
	public void setUp() throws Exception {
		shardStrategy = new IdHashShardingStrategy();
		shardStrategy.initialize( null, new DirectoryProvider[] {
				new RAMDirectoryProvider(), new RAMDirectoryProvider() } );
	}

	@Test
	public void testHashOverflow() {
		String key = String.valueOf( Integer.MAX_VALUE - 1 );
		// any key will do as long as it's hash is negative
		assertTrue( key.hashCode() < 0 );
		assertAcceptableId( key );
	}

	private void assertAcceptableId(String id) {
		shardStrategy.getDirectoryProviderForAddition( null, id, id, null );
		shardStrategy.getDirectoryProvidersForDeletion( null, id, id );
	}

}
