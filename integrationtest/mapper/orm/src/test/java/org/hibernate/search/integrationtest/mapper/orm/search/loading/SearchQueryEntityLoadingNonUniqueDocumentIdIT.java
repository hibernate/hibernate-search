/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SearchQueryEntityLoadingNonUniqueDocumentIdIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void nonUniqueDocumentId() {
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
