/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.fieldoncollection;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.embedded.depth.Person;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HSEARCH-1030")
public class LazyIndirectCollectionBridgeReindexTest extends SearchTestBase {

	@Test
	public void testLazyIndirectCollectionBridgeReindex() throws InterruptedException {
		prepareEntities();
		verifyMatchExistsWithName( "name", "name" );

		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			MassIndexer massIndexer = fullTextSession.createIndexer( Root.class );
			massIndexer.startAndWait();
		}
		verifyMatchExistsWithName( "name", "name" );
	}

	private void verifyMatchExistsWithName(String fieldName, String fieldValue) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new TermQuery( new Term( fieldName, fieldValue ) );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			int resultSize = fullTextQuery.getResultSize();
			assertEquals( 1, resultSize );

			@SuppressWarnings("unchecked")
			List<Person> list = fullTextQuery.list();
			assertEquals( 1, list.size() );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	private void prepareEntities() {
		Session session = openSession();
		try {
			Transaction transaction = session.beginTransaction();

			CollectionItem bridgedEntity = new CollectionItem();
			session.save( bridgedEntity );

			Leaf leaf = new Leaf();
			leaf.getCollectionItems().add( bridgedEntity );
			session.save( leaf );

			Root root = new Root();
			root.setName( "name" );
			root.setLeaf( leaf );
			session.save( root );

			transaction.commit();
		}
		finally {
			session.close();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CollectionItem.class, Leaf.class, Root.class };
	}

}
