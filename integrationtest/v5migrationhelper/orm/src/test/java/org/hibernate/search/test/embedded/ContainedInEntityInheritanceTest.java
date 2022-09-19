/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2486")
public class ContainedInEntityInheritanceTest extends SearchTestBase {

	@Test
	public void testContainedInIsInherited() {
		// Initialize
		Containing containing;
		try ( Session session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			containing = new Containing();
			session.save( containing );

			Contained contained = new Contained();
			containing.contained = contained;
			contained.containing = containing;
			session.save( contained );
			session.save( containing );

			transaction.commit();
		}
		assertEquals( 1, queryResultSize( 0 ) );

		// Update and test the containedIn
		try ( Session session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			containing = session.byId( Containing.class ).load( containing.id );
			containing.contained.field = 1;
			session.save( containing.contained );

			transaction.commit();
		}
		assertEquals( 1, queryResultSize( 1 ) );
		assertEquals( 0, queryResultSize( 0 ) );
	}

	private int queryResultSize(int fieldValue) {
		try ( Session session = openSession() ) {
			FullTextSession ftSession = Search.getFullTextSession( session );
			QueryBuilder builder = ftSession.getSearchFactory().buildQueryBuilder()
					.forEntity( Containing.class ).get();
			Query luceneQuery = builder.keyword().onField( "contained.field" ).matching( fieldValue ).createQuery();
			FullTextQuery query = ftSession.createFullTextQuery( luceneQuery, Containing.class );
			return query.getResultSize();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Containing.class, AbstractContained.class, Contained.class };
	}

	@Indexed
	@Entity(name = "containing")
	private static class Containing {
		@Id
		@GeneratedValue
		private long id;

		@OneToOne
		@IndexedEmbedded
		private Contained contained;
	}

	@MappedSuperclass
	private static class AbstractContained {
		@OneToOne(mappedBy = "contained")
		Containing containing;

		@Field
		int field;
	}

	/**
	 * This entity doesn't carry any Search specific annotation,
	 * but its superclass does.
	 */
	@Entity(name = "contained")
	private static class Contained extends AbstractContained {
		@Id
		@GeneratedValue
		private long id;
	}


}
