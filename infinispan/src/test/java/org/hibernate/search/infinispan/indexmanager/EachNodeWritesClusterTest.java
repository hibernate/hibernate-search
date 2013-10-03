/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
import static org.hibernate.search.infinispan.ClusterTestHelper.createClusterNode;

import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.infinispan.SimpleEmail;
import org.hibernate.search.test.util.FullTextSessionBuilder;

/**
 * In this configuration each node of the cluster will write in turn,
 * not using a clustered backend. Lock conflicts don't happen because
 * the test is single threaded, and it won't write on the next node
 * until the current change has been flushed.
 *
 * This configuration is used to simplify debugging only: never
 * use such a configuration on a production system unless your
 * application uses external locks to coordinate exclusive writes
 * to each node.
 *
 * Use these options to verify this test from your IDE:
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1 -XX:MaxPermSize=256m
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */

public class EachNodeWritesClusterTest extends BaseLiveClusterTest {

	/**
	 * Current known number of indexed emails.
	 */
	private int storedEmailsCount = 0;

	@Override
	protected void runVerifiers(List<FullTextSessionBuilder> nodes) {
		writeOnEachNode( nodes );
		verifyReadsOnEachNode( nodes );
	}

	@Override
	protected FullTextSessionBuilder createNewNode() {
		// disable exclusive mode to allow graceful failover:
		return createClusterNode( entityTypes, false );
	}

	private void writeOnEachNode(List<FullTextSessionBuilder> nodes) {
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

	private void verifyReadsOnEachNode(List<FullTextSessionBuilder> nodes) {
		for ( FullTextSessionBuilder builder : nodes ) {
			FullTextSession fullTextSession = builder.openFullTextSession();
			try {
				verifyReads( fullTextSession );
			}
			finally {
				fullTextSession.close();
			}
		}
	}

	private void verifyReads(FullTextSession session) {
		FullTextQuery fullTextQuery = session.createFullTextQuery( new MatchAllDocsQuery() );
		int resultSize = fullTextQuery.getResultSize();
		//total amount of emails in the system
		assertEquals( storedEmailsCount, resultSize );
		System.out.println( "Success! found " + storedEmailsCount + " matching emails in the Lucene index." );
		Query numericQuery = NumericFieldUtils.createExactMatchQuery( "sequential", ( storedEmailsCount - 1 ) );
		FullTextQuery fullTextNumericQuery = session.createFullTextQuery( numericQuery );
		int numericQueryresultSize = fullTextNumericQuery.getResultSize();
		//last written email
		assertEquals( 1, numericQueryresultSize );
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

}
