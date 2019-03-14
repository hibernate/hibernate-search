/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.Arrays;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the compatibility layer between our APIs and Hibernate ORM APIs.
 */
public class ToHibernateOrmIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setText( "this is text (1)" );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setText( "some more text (2)" );

			session.persist( entity1 );
			session.persist( entity2 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "text", entity1.getText() )
					)
					.add( "2", b -> b
							.field( "text", entity2.getText() )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void toHibernateOrmSession() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );
			assertThat( searchSession.toOrmSession() ).isSameAs( session );
		} );
	}

	@Test
	public void toHibernateOrmQuery() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );
			Query<IndexedEntity> query = createSimpleQuery( searchSession ).toOrmQuery();
			assertThat( query ).isNotNull();
		} );
	}

	@Test
	public void list() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );
			Query<IndexedEntity> query = createSimpleQuery( searchSession ).toOrmQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							6L,
							reference( IndexedEntity.INDEX, "1" ),
							reference( IndexedEntity.INDEX, "2" )
					)
			);
			List<IndexedEntity> result = query.list();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.containsExactly(
							session.getReference( IndexedEntity.class, 1 ),
							session.getReference( IndexedEntity.class, 2 )
					);
		} );
	}

	@Test
	public void uniqueResult() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );
			Query<IndexedEntity> query = createSimpleQuery( searchSession ).toOrmQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( IndexedEntity.INDEX, "1" )
					)
			);
			IndexedEntity result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( session.getReference( IndexedEntity.class, 1 ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.empty()
			);
			result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result ).isNull();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.INDEX, "1" ),
							reference( IndexedEntity.INDEX, "2" )
					)
			);
			SubTest.expectException( () -> {
				query.uniqueResult();
			} )
					.assertThrown()
					.isInstanceOf( org.hibernate.NonUniqueResultException.class );
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void pagination() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );
			Query<IndexedEntity> query = createSimpleQuery( searchSession ).toOrmQuery();

			assertThat( query.getFirstResult() ).isEqualTo( 0 );
			assertThat( query.getMaxResults() ).isEqualTo( Integer.MAX_VALUE );

			query.setFirstResult( 3 );
			query.setMaxResults( 2 );

			assertThat( query.getFirstResult() ).isEqualTo( 3 );
			assertThat( query.getMaxResults() ).isEqualTo( 2 );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b
							.firstResultIndex( 3L )
							.maxResultsCount( 2L ),
					StubSearchWorkBehavior.empty()
			);
			query.list();
			backendMock.verifyExpectationsMet();
		} );
	}

	private SearchQuery<IndexedEntity> createSimpleQuery(SearchSession searchSession) {
		return searchSession.search( IndexedEntity.class )
				.asEntity()
				.predicate( f -> f.matchAll() )
				.toQuery();
	}

	@Entity
	@Table(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@FullTextField(analyzer = "myAnalyzer")
		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}

}
