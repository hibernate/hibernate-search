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
import static org.hibernate.search.infinispan.ClusterTestHelper.createClusterNode;
import static org.hibernate.search.infinispan.ClusterTestHelper.waitMembersCount;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.infinispan.ClusterSharedConnectionProvider;
import org.hibernate.search.infinispan.SimpleEmail;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Start several nodes using the custom IndexManager and verify
 * communication by doing indexing and query operations on
 * each node.
 *
 * Use these options to verify this test from your IDE:
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1 -XX:MaxPermSize=256m
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-882")
public class BaseLiveClusterTest {

	private static final int TEST_RUNS = 33;
	private static final int MAX_NODES = 8;
	protected static HashSet<Class<?>> entityTypes;

	private final List<FullTextSessionBuilder> nodes = new LinkedList<FullTextSessionBuilder>();

	/**
	 * We'll have the cluster grow initially, to then start shrinking and growing again.
	 */
	private boolean growCluster = true;

	private int storedEmailsCount = 0;

	@Test
	public void liveRun() {
		try {
			for ( int i = 0; i < TEST_RUNS; i++ ) {
				adjustNodesNumber( i );
				writeOnEachNode();
				assertReads();
			}
		}
		finally {
			for ( FullTextSessionBuilder slave : nodes ) {
				slave.close();
			}
		}
	}

	private void assertReads() {
		for ( FullTextSessionBuilder slave : nodes ) {
			assertView( slave );
		}
	}

	private void assertView(FullTextSessionBuilder node) {
		assertEquals( nodes.size() , clusterSize( node, SimpleEmail.class ) );
		FullTextSession session = node.openFullTextSession();
		try {
			FullTextQuery fullTextQuery = session.createFullTextQuery( new MatchAllDocsQuery() );
			int resultSize = fullTextQuery.getResultSize();
			//total amount of emails in the system
			assertEquals( storedEmailsCount, resultSize );
			Query numericQuery = NumericFieldUtils.createExactMatchQuery( "sequential", ( storedEmailsCount - 1 ) );
			FullTextQuery fullTextNumericQuery = session.createFullTextQuery( numericQuery );
			int numericQueryresultSize = fullTextNumericQuery.getResultSize();
			//last written email
			assertEquals( 1, numericQueryresultSize );
		}
		finally {
			session.close();
		}
	}

	private void adjustNodesNumber(int i) {
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
		waitForAllJoinsCompleted();
	}

	private void addNewNode() {
		nodes.add( createNewNode() );
	}

	private void killSomeNode() {
		//we remove the oldest one: makes sure the master role rotates
		FullTextSessionBuilder sessionBuilder = nodes.remove( 0 );
		sessionBuilder.close();
	}

	private void writeOnEachNode() {
		for ( FullTextSessionBuilder builder : nodes ) {
			FullTextSession fullTextSession = builder.openFullTextSession();
			try {
				writeOnNode( fullTextSession );
			}
			finally {
				fullTextSession.close();
			}
		}
	}

	private void writeOnNode(FullTextSession fullTextSession) {
		Transaction transaction = fullTextSession.beginTransaction();
		SimpleEmail simpleEmail = new SimpleEmail();
		simpleEmail.to = "outher space";
		simpleEmail.message = "anybody out there?";
		simpleEmail.sequential = storedEmailsCount;
		fullTextSession.save( simpleEmail );
		transaction.commit();
		storedEmailsCount++;
	}

	private void waitForAllJoinsCompleted() {
		int expectedSize = nodes.size();
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

	protected FullTextSessionBuilder createNewNode() {
		return createClusterNode( entityTypes, false );
	}

}
