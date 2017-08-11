/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class OrmFilterTest extends SearchTestBase {

	// Just to check that our Hibernate ORM filter actually works
	@Test
	public void jpaQuery() {
		try ( Session session = openSession() ) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<FilterableEntity> criteria = cb.createQuery( FilterableEntity.class );
			Root<FilterableEntity> root = criteria.from( FilterableEntity.class );
			criteria.where( cb.like( root.get( "textValue" ), "%foo%" ) );
			criteria.orderBy( cb.asc( root.get( "id" ) ) );
			org.hibernate.query.Query<FilterableEntity> query = session.createQuery( criteria );
			query.setMaxResults( 3 );
			assertThat( query.getResultList() ).onProperty( "id" )
					.containsExactly( 0, 2, 3 );

			session.enableFilter( "filter1" ).setParameter( "excludedNumericValue", 3 );
			assertThat( query.getResultList() ).onProperty( "id" )
					.containsExactly( 0, 2, 4 );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-2842" )
	public void fullTextQuery() {
		try ( Session session = openSession(); FullTextSession fullTextSession = Search.getFullTextSession( session ) ) {
			QueryBuilder qb = getSearchFactory().buildQueryBuilder().forEntity( FilterableEntity.class ).get();
			Query query = qb.keyword().onField( "textValue" ).matching( "foo" ).createQuery();
			FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, FilterableEntity.class );
			ftQuery.setSort( qb.sort().byField( "idSort" ).createSort() );
			ftQuery.setMaxResults( 3 );
			assertThat( ftQuery.getResultSize() ).isEqualTo( 4 ); // Ignores max results, that's expected
			assertThat( ftQuery.getResultList() ).onProperty( "id" )
					.containsExactly( 0, 2, 3 );

			// TODO we'll need to change the following assertions when fixing HSEARCH-2848.

			fullTextSession.enableFilter( "filter1" ).setParameter( "excludedNumericValue", 3 );
			// Using a Hibernate ORM filter won't affect the returned result size
			assertThat( ftQuery.getResultSize() ).isEqualTo( 4 );
			/*
			 * Using a Hibernate ORM filter will affect the returned result list,
			 * but the filter will be applied after the result limit.
			 * Thus the result 4 is missing here, while we would expect it to be included.
			 */
			assertThat( ftQuery.getResultList() ).onProperty( "id" )
					.containsExactly( 0, 2 );
		}
	}

	@Before
	public void createData() {
		try ( Session s = openSession() ) {
			s.getTransaction().begin();
			s.persist( new FilterableEntity( 0, "foo", 0 ) );
			s.persist( new FilterableEntity( 1, "bar", 1 ) );
			s.persist( new FilterableEntity( 2, "foo bar", 2 ) );
			s.persist( new FilterableEntity( 3, "foo bar", 3 ) );
			s.persist( new FilterableEntity( 4, "foo bar", 4 ) );
			s.getTransaction().commit();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				FilterableEntity.class,
		};
	}

	@Entity
	@Indexed
	@FilterDef(name = "filter1", parameters = @ParamDef(name = "excludedNumericValue", type = "integer"))
	@org.hibernate.annotations.Filter(name = "filter1", condition = "numericValue <> :excludedNumericValue")
	public static class FilterableEntity {
		@Id
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		private Integer id;

		@Field
		@Basic
		private String textValue;

		// NOT indexed (only accessible through Hibernate ORM filters)
		@Basic
		private Integer numericValue;

		protected FilterableEntity() {
		}

		public FilterableEntity(Integer id, String textValue, Integer numericValue) {
			this.id = id;
			this.textValue = textValue;
			this.numericValue = numericValue;
		}

		public Integer getId() {
			return id;
		}
	}
}
