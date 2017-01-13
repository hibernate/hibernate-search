/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.path;

import static org.junit.Assert.assertEquals;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;


/**
 * Test the behavior when an {@literal @IndexedEmbedded} with default paths (i.e. "include everything")
 * has another, nested {@literal @IndexedEmbedded} with non-default paths (i.e. "include those paths only").
 *
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2547")
public class DefaultPathsWithNestedIndexedEmbeddedTest extends SearchTestBase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { A.class, B.class, C.class };
	}

	@Test
	public void testIndexAndSearch() {
		try (Session session = openSession()) {
			FullTextSession fts = Search.getFullTextSession( session );

			Transaction tx = fts.beginTransaction();
			A a = new A();
			a.foo = "someValue";
			B b = new B();
			b.a = a;
			C c = new C();
			c.b = b;
			fts.persist( a );
			fts.persist( b );
			fts.persist( c );
			tx.commit();
			fts.clear();

			tx = fts.beginTransaction();
			Query query = new TermQuery( new Term( "b.a.foo", a.foo ) );
			assertEquals( 1, fts.createFullTextQuery( query, C.class ).getResultSize() );
			tx.commit();
			fts.clear();
		}
	}

	@Entity
	@Indexed
	private static class A {
		@Id
		@GeneratedValue
		private Long id;

		@Field(analyze = Analyze.NO)
		private String foo;
	}

	@Entity
	private static class B {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@IndexedEmbedded(includePaths = "foo") // Include only "a.foo"
		private A a;
	}

	@Entity
	@Indexed
	private static class C {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@IndexedEmbedded // Include every field of "b"
		private B b;
	}
}
