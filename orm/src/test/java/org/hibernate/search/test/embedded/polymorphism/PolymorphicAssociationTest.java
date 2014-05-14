/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.polymorphism;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HSEARCH-1241")
public class PolymorphicAssociationTest extends SearchTestBase {

	private static final String INIT_NAME = "initname";
	private static final String EDIT_NAME = "editname";

	@Test
	public void testPolymorphicAssociation() {
		prepareEntities( INIT_NAME );

		// Here the test can fail if the polymorphic association is not properly handled.
		// In this case, an exception will be thrown because of a failed field access on a proxy object.
		changeLevel3Name( INIT_NAME, EDIT_NAME );
	}

	private void prepareEntities(String level3Name) {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();

			Level1 level1 = new Level1();
			DerivedLevel2 level2 = new DerivedLevel2();
			Level3 level3 = new Level3();

			level1.setLevel2Child( level2 );
			level2.setLevel1Parent( level1 );
			level2.setLevel3Child( level3 );
			level3.setLevel2Parent( level2 );

			level3.setName( level3Name );

			session.save( level1 );
			session.save( level2 );
			session.save( level3 );

			transaction.commit();
		}
		finally {
			session.close();
		}
	}

	private void changeLevel3Name(String currentName, String newName) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new TermQuery( new Term( "name", currentName ) );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			assertEquals( 1, fullTextQuery.getResultSize() );

			Level3 level3 = (Level3) fullTextQuery.list().get( 0 );
			level3.setName( newName );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Level1.class, Level2.class, DerivedLevel2.class, Level3.class };
	}

}
