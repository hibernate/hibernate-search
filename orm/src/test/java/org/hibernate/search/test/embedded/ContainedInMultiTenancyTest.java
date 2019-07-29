/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import org.apache.lucene.search.Query;

@RequiresDialect(
		comment = "The connection provider for this test ignores configuration and requires H2",
		strictMatching = true,
		value = org.hibernate.dialect.H2Dialect.class
)
public class ContainedInMultiTenancyTest extends SearchTestBase {

	private static final String TENANT_ID_1 = "tenant1";
	private static final String TENANT_ID_2 = "tenant2";

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3647")
	public void testContainedInProcessingRemembersTenantId() {
		// Initialize
		Containing containing;
		try ( Session session = openSession( TENANT_ID_1 ) ) {
			Transaction transaction = session.beginTransaction();

			containing = new Containing();
			containing.setId( 1L );

			ContainedLevel1 contained1 = new ContainedLevel1();
			contained1.setId( 2L );
			containing.setContained( contained1 );
			contained1.setContaining( containing );

			ContainedLevel2 contained2 = new ContainedLevel2();
			contained2.setId( 3L );
			contained2.setField( 1 );
			contained1.setContained( contained2 );
			contained2.setContaining( contained1 );

			session.persist( contained2 );
			session.persist( contained1 );
			session.persist( containing );

			transaction.commit();
		}
		assertEquals( 1, queryResultSize( TENANT_ID_1, 1 ) );
		assertEquals( 0, queryResultSize( TENANT_ID_2, 1 ) );

		// Update and test the containedIn
		try ( Session session = openSession( TENANT_ID_1 ) ) {
			Transaction transaction = session.beginTransaction();

			ContainedLevel2 contained = session.getReference( ContainedLevel2.class, 3L );
			contained.setField( 2 );

			transaction.commit();
		}
		// This used to fail because the containing entity was reindexed without any tenant ID
		assertEquals( 1, queryResultSize( TENANT_ID_1, 2 ) );
		assertEquals( 0, queryResultSize( TENANT_ID_2, 2 ) );

		// Also check we correctly removed the old document from the index
		assertEquals( 0, queryResultSize( TENANT_ID_1, 1 ) );
	}

	private Session openSession(String tenantId) {
		return getSessionFactory().withOptions()
				.tenantIdentifier( tenantId )
				.openSession();
	}

	private int queryResultSize(String tenantId, int fieldValue) {
		try ( Session session = openSession( tenantId ) ) {
			FullTextSession ftSession = Search.getFullTextSession( session );
			QueryBuilder builder = ftSession.getSearchFactory().buildQueryBuilder()
					.forEntity( Containing.class ).get();
			Query luceneQuery = builder.keyword().onField( "contained.contained.field" ).matching( fieldValue ).createQuery();
			FullTextQuery query = ftSession.createFullTextQuery( luceneQuery, Containing.class );
			return query.getResultSize();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Containing.class, ContainedLevel1.class, ContainedLevel2.class };
	}

	@Override
	public Set<String> multiTenantIds() {
		return new HashSet<>( Arrays.asList( TENANT_ID_1, TENANT_ID_2 ) );
	}

	@Indexed
	@Entity(name = "containing")
	private static class Containing {
		@Id
		private long id;

		@OneToOne
		@IndexedEmbedded
		private ContainedLevel1 contained;

		protected Containing() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public ContainedLevel1 getContained() {
			return contained;
		}

		public void setContained(ContainedLevel1 contained) {
			this.contained = contained;
		}
	}

	@Entity(name = "contained1")
	private static class ContainedLevel1 {
		@Id
		private long id;

		@OneToOne(mappedBy = "contained")
		@ContainedIn
		private Containing containing;

		@OneToOne
		@IndexedEmbedded
		private ContainedLevel2 contained;

		protected ContainedLevel1() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Containing getContaining() {
			return containing;
		}

		public void setContaining(Containing containing) {
			this.containing = containing;
		}

		public ContainedLevel2 getContained() {
			return contained;
		}

		public void setContained(ContainedLevel2 contained) {
			this.contained = contained;
		}
	}


	@Entity(name = "contained2")
	private static class ContainedLevel2 {
		@Id
		private long id;

		@Basic
		@Field
		private int field;

		@OneToOne(mappedBy = "contained")
		@ContainedIn
		private ContainedLevel1 containing;

		protected ContainedLevel2() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public int getField() {
			return field;
		}

		public void setField(int field) {
			this.field = field;
		}

		public ContainedLevel1 getContaining() {
			return containing;
		}

		public void setContaining(ContainedLevel1 containing) {
			this.containing = containing;
		}
	}

}
