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
 * Test executing a search query and loading entities whose document ID is not the entity ID.
 */
public class SearchQueryNonEntityIdDocumentIdIT {

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

		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void asEntity() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.getSearchSession( session );

			SearchQuery<IndexedEntity> query = searchSession.search( IndexedEntity.class )
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							reference( IndexedEntity.INDEX, "41" ),
							reference( IndexedEntity.INDEX, "42" ),
							reference( IndexedEntity.INDEX, "43" )
					)
			);

			Assertions.assertThat( query.fetchHits() ).containsExactly(
					session.getReference( IndexedEntity.class, 1 ),
					session.getReference( IndexedEntity.class, 2 ),
					session.getReference( IndexedEntity.class, 3 )
			);
		} );
	}

	private void initData() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new IndexedEntity( 1, 41 ) );
			session.persist( new IndexedEntity( 2, 42 ) );
			session.persist( new IndexedEntity( 3, 43 ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "41", b -> { } )
					.add( "42", b -> { } )
					.add( "43", b -> { } )
					.preparedThenExecuted();
		} );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "Indexed")
	@Table
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@DocumentId
		private Integer documentId;

		protected IndexedEntity() {
		}

		public IndexedEntity(int id, int documentId) {
			this.id = id;
			this.documentId = documentId;
		}

		public Integer getId() {
			return id;
		}
	}
}
