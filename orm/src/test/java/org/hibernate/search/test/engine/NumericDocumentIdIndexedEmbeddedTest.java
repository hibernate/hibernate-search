/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

public class NumericDocumentIdIndexedEmbeddedTest extends SearchInitializationTestBase {

	@TestForIssue(jiraKey = "HSEARCH-2545")
	@Test
	public void testIndexAndSearchNumericField() {
		init( A.class, B.class, C.class );
		/*
		 * We mainly want to test that the search factory will initialize without error,
		 * but also checking that the field is actually numeric won't hurt.
		 */
		try ( Session session = getTestResourceManager().openSession() ) {
			FullTextSession fts = Search.getFullTextSession( session );

			Transaction tx = fts.beginTransaction();
			A a = new A();
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
			// Range Queries including lower and upper bounds
			assertEquals( "Query id ", 1, numericQueryFor( fts, C.class, "b.a.id", a.id, a.id ).size() );
			tx.commit();
			fts.clear();
		}
	}

	private List<?> numericQueryFor(FullTextSession fullTextSession, Class<?> entityClass, String fieldName, Object from, Object to) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, from, to, true, true );
		return fullTextSession.createFullTextQuery( query, entityClass ).list();
	}

	@Entity
	@Indexed
	private static class A {
		@Id
		@GeneratedValue
		@NumericField
		@SortableField
		private Long id;
	}

	@Entity
	private static class B {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@IndexedEmbedded(includePaths = "id")
		private A a;
	}

	@Entity
	@Indexed
	private static class C {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@IndexedEmbedded(includePaths = "a.id")
		private B b;
	}

}
