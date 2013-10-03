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
package org.hibernate.search.infinispan.indexmanager;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.search.infinispan.ClusterTestHelper.clusterSize;
import static org.hibernate.search.infinispan.ClusterTestHelper.waitMembersCount;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.infinispan.ClusterSharedConnectionProvider;
import org.hibernate.search.infinispan.SimpleEmail;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Start several nodes using the custom Infinispan Directory and verify
 * communication by doing indexing and query operations on each node.
 *
 * This test disables exclusive mode and nodes are not killed during write
 * operations, so locks cleanup is not covered in this configuration.
 *
 * Each node is writing directly to the index: there is no forwarding of
 * write operations; this works fine as they write on turns so never
 * generate contention on the index lock.
 *
 * This configuration doesn't make sense in practice: the intention is
 * to verify the test infrastructure and then to be extended to run on
 * nodes with a more interesting configurations.
 *
 * Use these options to verify this test from your IDE:
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1 -XX:MaxPermSize=256m
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public abstract class BaseLiveClusterTest {

	/**
	 * This is a stress test and provides a level
	 * of confidence proportional to the amount of
	 * iterations performed.
	 */
	private static final int TEST_RUNS = 66;

	/**
	 * We scale from one node to this constant, then we start
	 * scaling back by shutting down nodes. Value should be
	 * large enough to test a reasonably sized cluster: there
	 * is no point in testing large clusters but at least
	 * more than DIST num owners should be used to catch
	 * all possible cluster configurations.
	 */
	private static final int MAX_NODES = 7;

	/**
	 * At each iteration between the cluster reconfiguration
	 * and the read/write operations we can wait for the cluster
	 * to be strictly verified from the point of view of each
	 * node.
	 * Waiting shouldn't be needed but makes debugging a failing
	 * test much easier.
	 */
	private static final boolean WAIT_CLUSTER_FORMATION = false;

	protected static HashSet<Class<?>> entityTypes;

	private final List<FullTextSessionBuilder> nodes = new LinkedList<FullTextSessionBuilder>();

	/**
	 * We'll have the cluster grow initially, to then start shrinking and growing again.
	 */
	private boolean growCluster = true;

	@Test
	public void liveRun() {
		try {
			for ( int i = 0; i < TEST_RUNS; i++ ) {
				adjustNodesNumber( i );
				assertViews();
				runVerifiers( nodes );
			}
		}
		finally {
			for ( FullTextSessionBuilder node : nodes ) {
				node.close();
			}
		}
	}

	protected abstract void runVerifiers(List<FullTextSessionBuilder> clusterNodes);

	private void assertViews() {
		if ( WAIT_CLUSTER_FORMATION ) {
			for ( FullTextSessionBuilder node : nodes ) {
				assertEquals( nodes.size() , clusterSize( node, SimpleEmail.class ) );
			}
		}
	}

	private void adjustNodesNumber(final int i) {
		if ( growCluster ) {
			if ( nodes.size() >= MAX_NODES ) {
				growCluster = false;
				killSomeNode();
			}
			else {
				addNewNode();
			}
		}
		else {
			if ( nodes.size() == 1 ) {
				growCluster = true;
				addNewNode();
			}
			else {
				killSomeNode();
			}
		}
		if ( WAIT_CLUSTER_FORMATION ) {
			waitForAllJoinsCompleted();
		}
	}

	private void addNewNode() {
		nodes.add( createNewNode() );
		System.out.println( "Added new node to the cluster" );
	}

	private void killSomeNode() {
		//we remove the oldest one: makes sure the master role rotates
		FullTextSessionBuilder sessionBuilder = nodes.remove( 0 );
		sessionBuilder.close();
		System.out.println( "Removed a node from the cluster" );
	}

	private void waitForAllJoinsCompleted() {
		final int expectedSize = nodes.size();
		for ( FullTextSessionBuilder slave : nodes ) {
			waitMembersCount( slave, SimpleEmail.class, expectedSize );
		}
	}

	@BeforeClass
	public static void prepareConnectionPool() {
		entityTypes = new HashSet<Class<?>>();
		entityTypes.add( SimpleEmail.class );
		ClusterSharedConnectionProvider.realStart();
	}

	@AfterClass
	public static void shutdownConnectionPool() {
		ClusterSharedConnectionProvider.realStop();
	}

	protected abstract FullTextSessionBuilder createNewNode();

}
