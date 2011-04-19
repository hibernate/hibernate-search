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

import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * In this test we initially start a master node which will stay alive
 * for the full test duration and constantly indexing new entities.
 * 
 * After that we add and remove additional new nodes, still making more
 * index changes checking that each node is always able
 * to see changes as soon as committed by the main node; this
 * results in a very stressfull test as the cluster topology is changed
 * at each step (though it doesn't rehash as it's replicating).
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class LiveRunningTest {
	
	private static final int TEST_RUNS = 70;
	private static final int MAX_SLAVES = 5;
	
	private final FullTextSessionBuilder master = createClusterNode();
	private final List<FullTextSessionBuilder> slaves = new LinkedList<FullTextSessionBuilder>();
	
	private boolean growCluster = true;
	
	private int storedEmailsCount = 0;
	
	@Test
	public void liveRun() {
		try {
			for ( int i = 0; i < TEST_RUNS; i++ ) {
			writeOnMaster();
			adjustSlavesNumber(i);
			assertViews();
		}
		}
		finally {
			master.close();
			for ( FullTextSessionBuilder slave : slaves ) {
				slave.close();
			}
		}
	}

	private void assertViews() {
		assertView( master );
		for ( FullTextSessionBuilder slave : slaves ) {
			assertView( slave );
		}
	}

	private void assertView(FullTextSessionBuilder node) {
		Assert.assertEquals( slaves.size() + 1 , TwoNodesTest.clusterSize( node ) );
		FullTextSession session = node.openFullTextSession();
		try {
			FullTextQuery fullTextQuery = session.createFullTextQuery( new MatchAllDocsQuery() );
			int resultSize = fullTextQuery.getResultSize();
			Assert.assertEquals( storedEmailsCount, resultSize );
		}
		finally{
			session.close();
		}
	}

	private void adjustSlavesNumber(int i) {
		if ( growCluster ) {
			if ( slaves.size() >= MAX_SLAVES ) {
				growCluster = false;
			}
			else {
				slaves.add( createClusterNode() );
			}
		}
		else {
			if ( slaves.size() == 0 ) {
				growCluster = true;
			}
			else {
				FullTextSessionBuilder sessionBuilder = slaves.remove( 0 );
				sessionBuilder.close();
			}
		}
		waitForAllJoinsCompleted();
	}

	private void writeOnMaster() {
		FullTextSession fullTextSession = master.openFullTextSession();
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			SimpleEmail simpleEmail = new SimpleEmail();
			simpleEmail.to = "outher space";
			simpleEmail.message = "anybody out there?";
			fullTextSession.save( simpleEmail );
			transaction.commit();
			storedEmailsCount++;
		}
		finally {
			fullTextSession.close();
		}
	}
	
	private void waitForAllJoinsCompleted() {
		int expectedSize = slaves.size() + 1;
		TwoNodesTest.waitMembersCount( master, expectedSize );
		for (FullTextSessionBuilder slave : slaves) {
			TwoNodesTest.waitMembersCount( slave, expectedSize );
		}
	}

	private FullTextSessionBuilder createClusterNode() {
		FullTextSessionBuilder node = new FullTextSessionBuilder()
			.setProperty( "hibernate.search.default.directory_provider", "infinispan" )
			// fragment on every 7 bytes: don't use this on a real case!
			// only done to make sure we generate lots of small fragments.
			.setProperty( "hibernate.search.default.chunk_size", "7" )
			// this schema is shared across nodes, so don't drop it on shutdown:
			.setProperty( Environment.HBM2DDL_AUTO, "create" )
			.setProperty(
					CacheManagerServiceProvider.INFINISPAN_CONFIGURATION_RESOURCENAME,
					"testing-hibernatesearch-infinispan.xml" )
			// share the same in-memory database connection pool
			.setProperty(
					Environment.CONNECTION_PROVIDER,
					org.hibernate.search.infinispan.ClusterSharedConnectionProvider.class.getName()
					)
			.addAnnotatedClass( SimpleEmail.class );
		return node.build();
	}
	
	@BeforeClass
	public static void prepareConnectionPool() {
		ClusterSharedConnectionProvider.realStart();
	}
	
	@AfterClass
	public static void shutdownConnectionPool() {
		ClusterSharedConnectionProvider.realStop();
	}

}
