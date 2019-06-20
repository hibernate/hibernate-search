/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search;

import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.Arrays;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

/**
 * Test entity loading when executing a search query,
 * in particular when an entity type has a document ID that is not the entity ID.
 */
public class SearchQueryEntityLoadingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( EntityIdDocumentIdIndexedEntity.INDEX );
		backendMock.expectAnySchema( NonEntityIdDocumentIdIndexedEntity.INDEX );

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup(
						EntityIdDocumentIdIndexedEntity.class,
						NonEntityIdDocumentIdIndexedEntity.class
				);

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void entityIdDocumentId() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<EntityIdDocumentIdIndexedEntity> query = searchSession.search( EntityIdDocumentIdIndexedEntity.class )
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( EntityIdDocumentIdIndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							reference( EntityIdDocumentIdIndexedEntity.INDEX, "1" ),
							reference( EntityIdDocumentIdIndexedEntity.INDEX, "2" ),
							reference( EntityIdDocumentIdIndexedEntity.INDEX, "3" )
					)
			);

			Assertions.assertThat( query.fetchHits() ).containsExactly(
					session.getReference( EntityIdDocumentIdIndexedEntity.class, 1 ),
					session.getReference( EntityIdDocumentIdIndexedEntity.class, 2 ),
					session.getReference( EntityIdDocumentIdIndexedEntity.class, 3 )
			);
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void nonEntityIdDocumentId() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<NonEntityIdDocumentIdIndexedEntity> query = searchSession.search( NonEntityIdDocumentIdIndexedEntity.class )
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( NonEntityIdDocumentIdIndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							reference( NonEntityIdDocumentIdIndexedEntity.INDEX, "41" ),
							reference( NonEntityIdDocumentIdIndexedEntity.INDEX, "42" ),
							reference( NonEntityIdDocumentIdIndexedEntity.INDEX, "43" )
					)
			);

			Assertions.assertThat( query.fetchHits() ).containsExactly(
					session.getReference( NonEntityIdDocumentIdIndexedEntity.class, 1 ),
					session.getReference( NonEntityIdDocumentIdIndexedEntity.class, 2 ),
					session.getReference( NonEntityIdDocumentIdIndexedEntity.class, 3 )
			);
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void mixed() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Object> query = searchSession.search( Arrays.asList(
							EntityIdDocumentIdIndexedEntity.class,
							NonEntityIdDocumentIdIndexedEntity.class
					) )
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( EntityIdDocumentIdIndexedEntity.INDEX, NonEntityIdDocumentIdIndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							6L,
							reference( EntityIdDocumentIdIndexedEntity.INDEX, "1" ),
							reference( NonEntityIdDocumentIdIndexedEntity.INDEX, "41" ),
							reference( EntityIdDocumentIdIndexedEntity.INDEX, "2" ),
							reference( NonEntityIdDocumentIdIndexedEntity.INDEX, "42" ),
							reference( NonEntityIdDocumentIdIndexedEntity.INDEX, "43" ),
							reference( EntityIdDocumentIdIndexedEntity.INDEX, "3" )
					)
			);

			Assertions.assertThat( query.fetchHits() ).containsExactly(
					session.getReference( EntityIdDocumentIdIndexedEntity.class, 1 ),
					session.getReference( NonEntityIdDocumentIdIndexedEntity.class, 1 ),
					session.getReference( EntityIdDocumentIdIndexedEntity.class, 2 ),
					session.getReference( NonEntityIdDocumentIdIndexedEntity.class, 2 ),
					session.getReference( NonEntityIdDocumentIdIndexedEntity.class, 3 ),
					session.getReference( EntityIdDocumentIdIndexedEntity.class, 3 )
			);
		} );
	}

	private void initData() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new EntityIdDocumentIdIndexedEntity( 1 ) );
			session.persist( new EntityIdDocumentIdIndexedEntity( 2 ) );
			session.persist( new EntityIdDocumentIdIndexedEntity( 3 ) );
			session.persist( new NonEntityIdDocumentIdIndexedEntity( 1, 41 ) );
			session.persist( new NonEntityIdDocumentIdIndexedEntity( 2, 42 ) );
			session.persist( new NonEntityIdDocumentIdIndexedEntity( 3, 43 ) );

			backendMock.expectWorks( EntityIdDocumentIdIndexedEntity.INDEX )
					.add( "1", b -> { } )
					.add( "2", b -> { } )
					.add( "3", b -> { } )
					.preparedThenExecuted();
			backendMock.expectWorks( NonEntityIdDocumentIdIndexedEntity.INDEX )
					.add( "41", b -> { } )
					.add( "42", b -> { } )
					.add( "43", b -> { } )
					.preparedThenExecuted();
		} );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "EntityIdDocumentId")
	@Table
	@Indexed(index = EntityIdDocumentIdIndexedEntity.INDEX)
	public static class EntityIdDocumentIdIndexedEntity {

		public static final String INDEX = "EntityIdDocumentId";

		@Id
		private Integer id;

		protected EntityIdDocumentIdIndexedEntity() {
		}

		public EntityIdDocumentIdIndexedEntity(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "NonEntityIdDocumentId")
	@Table
	@Indexed(index = NonEntityIdDocumentIdIndexedEntity.INDEX)
	public static class NonEntityIdDocumentIdIndexedEntity {

		public static final String INDEX = "NonEntityIdDocumentId";

		@Id
		private Integer id;

		@DocumentId
		private Integer documentId;

		protected NonEntityIdDocumentIdIndexedEntity() {
		}

		public NonEntityIdDocumentIdIndexedEntity(int id, int documentId) {
			this.id = id;
			this.documentId = documentId;
		}

		public Integer getId() {
			return id;
		}
	}
}
