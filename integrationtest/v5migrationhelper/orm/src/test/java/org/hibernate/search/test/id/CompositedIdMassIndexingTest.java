/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * Verifies the MassIndexer can deal correctly with entities which have a composite id
 */
public class CompositedIdMassIndexingTest {

	@Rule
	public FullTextSessionBuilder ftsBuilder = new FullTextSessionBuilder()
			.addAnnotatedClass( RegistrationId.class )
			.addAnnotatedClass( StudentEntity.class )
			.build();

	@Test
	public void testReindexingWithCompositeIds() throws InterruptedException {
		try ( FullTextSession fullTextSession = ftsBuilder.openFullTextSession() ) {
			storeTestData( fullTextSession );
		}
		try ( FullTextSession fullTextSession = ftsBuilder.openFullTextSession() ) {
			assertEquals( 0, fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), StudentEntity.class ) );
		}
		try ( FullTextSession fullTextSession = ftsBuilder.openFullTextSession() ) {
			fullTextSession.createIndexer().startAndWait();
		}
		try ( FullTextSession fullTextSession = ftsBuilder.openFullTextSession() ) {
			assertEquals( 1, fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), StudentEntity.class ) );
		}
	}

	private void storeTestData(Session session) {
		Transaction tx = session.beginTransaction();
		RegistrationId firstId = new RegistrationId();
		firstId.setDepartment( "Software Engineering" );
		firstId.setStudentId( 1 );
		StudentEntity firstStudent = new StudentEntity();
		firstStudent.setRegid( firstId );
		firstStudent.setName( "I am noone" );
		session.save( firstStudent );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();
		Search.getFullTextSession( session ).purge( StudentEntity.class, firstId );
		tx.commit();
		session.clear();
	}

}
