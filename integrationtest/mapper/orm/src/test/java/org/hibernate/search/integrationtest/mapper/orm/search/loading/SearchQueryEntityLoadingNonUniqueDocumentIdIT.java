/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SearchQueryEntityLoadingNonUniqueDocumentIdIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void nonUniqueDocumentId() {
		backendMock.inLenientMode( () -> with( sessionFactory ).runInTransaction( session -> {
			for ( long i = 0; i < 2; i++ ) {
				IndexedEntity entity = new IndexedEntity();
				entity.setId( i );
				entity.setNonUniqueProperty( 0L );
				session.persist( entity );
			}
		} ) );

		assertThatThrownBy( () -> with( sessionFactory ).runInTransaction( session -> {
			backendMock.expectSearchObjects( IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, StubBackendUtils.reference( IndexedEntity.NAME, "0" ) ) );
			Search.session( session ).search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.fetchAllHits();
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple instances of entity type '" + IndexedEntity.NAME
						+ "' have their property 'nonUniqueProperty' set to '0'."
						+ " 'nonUniqueProperty' is the document ID and must be assigned unique values." );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	private static class IndexedEntity {

		static final String NAME = "Indexed";

		@Id
		private Long id;

		@DocumentId
		private Long nonUniqueProperty;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getNonUniqueProperty() {
			return nonUniqueProperty;
		}

		public void setNonUniqueProperty(Long nonUniqueProperty) {
			this.nonUniqueProperty = nonUniqueProperty;
		}
	}

}
