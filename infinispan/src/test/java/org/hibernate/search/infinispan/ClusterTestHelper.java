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

package org.hibernate.search.infinispan;

import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.impl.InfinispanDirectoryProvider;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

/**
 * Helpers to setup several instances of Hibernate Search using
 * clustering to connect the index, and sharing the same H2
 * database instance.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ClusterTestHelper {

	/**
	 * Create a clustered Hibernate Search instance.
	 * Note the configuration used is not optimal for performance,
	 * we do this on purpose to make sure we test with an highly
	 * fragmented index.
	 * The backing CacheManager will be started, but didn't necessarily
	 * join the existing nodes.
	 * @return a started FullTextSessionBuilder
	 */
	public static FullTextSessionBuilder createClusterNode(Set<Class<?>> entityTypes, boolean exclusiveWrite) {
		FullTextSessionBuilder node = new FullTextSessionBuilder()
			.setProperty( "hibernate.search.default.directory_provider", "infinispan" )
			// fragment on every 7 bytes: don't use this on a real case!
			// only done to make sure we generate lots of small fragments.
			.setProperty( "hibernate.search.default.indexwriter.chunk_size", "13" )
			// this schema is shared across nodes, so don't drop it on shutdown:
			.setProperty( Environment.HBM2DDL_AUTO, "create" )
			// if we should allow aggressive index locking:
			.setProperty( "hibernate.search.default." + org.hibernate.search.Environment.EXCLUSIVE_INDEX_USE,
					String.valueOf( exclusiveWrite ) )
			// share the same in-memory database connection pool
			.setProperty(
					Environment.CONNECTION_PROVIDER,
					org.hibernate.search.infinispan.ClusterSharedConnectionProvider.class.getName()
					);
		for ( Class<?> entityType : entityTypes ) {
			node.addAnnotatedClass( entityType );
		}
		return node.build();
	}

	/**
	 * Wait some time for the cluster to form
	 */
	public static void waitMembersCount(FullTextSessionBuilder node, Class<?> entityType, int expectedSize) {
		int currentSize = 0;
		int loopCounter = 0;
		while ( currentSize < expectedSize ) {
			try {
				Thread.sleep( 10 );
			}
			catch ( InterruptedException e ) {
				throw new AssertionFailedError( e.getMessage() );
			}
			currentSize = clusterSize( node, entityType );
			if ( loopCounter > 200 ) {
				throw new AssertionFailedError( "timeout while waiting for all nodes to join in cluster" );
			}
		}
	}

	/**
	 * Counts the number of nodes in the cluster on this node
	 * @param node the FullTextSessionBuilder representing the current node
	 * @return the number of nodes as seen by the current node
	 */
	public static int clusterSize(FullTextSessionBuilder node, Class<?> entityType) {
		SearchFactoryIntegrator searchFactory = (SearchFactoryIntegrator) node.getSearchFactory();
		EntityIndexBinder indexBinding = searchFactory.getIndexBindingForEntity( entityType );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBinding.getIndexManagers()[0];
		InfinispanDirectoryProvider directoryProvider = (InfinispanDirectoryProvider) indexManager.getDirectoryProvider();
		EmbeddedCacheManager cacheManager = directoryProvider.getCacheManager();
		List<Address> members = cacheManager.getMembers();
		return members.size();
	}

}
