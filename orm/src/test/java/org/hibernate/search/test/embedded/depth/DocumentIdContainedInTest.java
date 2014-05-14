/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.depth;

import java.util.List;

import org.junit.Assert;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.testsupport.backend.LeakingLuceneBackend;

/**
 * @author Sanne Grinovero
 */
public class DocumentIdContainedInTest extends RecursiveGraphTest {

	@Override
	public void testCorrectDepthIndexed() {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();
			session.persist( new PersonWithBrokenSocialSecurityNumber( 1L, "Mario Rossi" ) );
			session.persist( new PersonWithBrokenSocialSecurityNumber( 2L, "Bruno Rossi" ) );
			transaction.commit();
		}
		finally {
			session.close();
		}
		List<LuceneWork> processedQueue = LeakingLuceneBackend.getLastProcessedQueue();
		// as they resolve to the same Lucene id only one instance will make it to the backend.
		// (which one is undefined, nobody should use a constant as id)
		Assert.assertEquals( 1, processedQueue.size() );
		Assert.assertEquals( "100", processedQueue.get( 0 ).getId() );
		Assert.assertTrue( processedQueue.get( 0 ) instanceof AddLuceneWork );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { PersonWithBrokenSocialSecurityNumber.class };
	}

}
