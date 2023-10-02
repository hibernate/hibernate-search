/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxPollingIndexingPlanFilterIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeAll
	public void setup() {
		backendMock
				.expectSchema( EntityA.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity2A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1B.INDEX, b -> b.field( "indexedField", String.class ) );

		sessionFactory = ormSetupHelper.start().withAnnotatedTypes(
				EntityA.class, Entity1A.class, Entity1B.class, Entity2A.class
		).setup();
	}

	@BeforeEach
	void clearFilter() throws Exception {
		Search.mapping( sessionFactory ).indexingPlanFilter(
				ctx -> { /*clear out any settings from tests*/ }
		);
	}

	@Test
	void partialSessionFilterFails() {
		with( sessionFactory ).runInTransaction( session -> {
			assertThatThrownBy( () -> Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( Entity1A.class ) )
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Unable to apply the given filter at the session level with the outbox polling coordination strategy.",
							"With this coordination strategy, applying a session-level indexing plan filter is only allowed if it excludes all types."
					);
		} );
	}

	@Test
	void allTypesMixSessionFilterFails() {
		with( sessionFactory ).runInTransaction( session -> {
			assertThatThrownBy( () -> Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( EntityA.class )
							.exclude( Entity1A.class )
							.exclude( Entity1B.class )
							.include( Entity2A.class ) )
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Unable to apply the given filter at the session level with the outbox polling coordination strategy.",
							"With this coordination strategy, applying a session-level indexing plan filter is only allowed if it excludes all types."
					);
		} );
	}

	@Test
	void allTypesExcludeExplicitlySessionFilter() {
		with( sessionFactory ).runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( EntityA.class )
							.exclude( Entity1A.class )
							.exclude( Entity1B.class )
							.exclude( Entity2A.class ) );

			session.persist( new EntityA( 1, "test" ) );
			session.persist( new Entity1A( 2, "test" ) );
			session.persist( new Entity1B( 3, "test" ) );
			session.persist( new Entity2A( 4, "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void allTypesExcludeInheritanceSessionFilter() {
		with( sessionFactory ).runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( EntityA.class )
			);

			session.persist( new EntityA( 10, "test" ) );
			session.persist( new Entity1A( 20, "test" ) );
			session.persist( new Entity1B( 30, "test" ) );
			session.persist( new Entity2A( 40, "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity
	@Indexed(index = EntityA.INDEX)
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class EntityA {

		static final String INDEX = "A";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public EntityA() {
		}

		public EntityA(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

	@Entity
	@Indexed(index = Entity1A.INDEX)
	public static class Entity1A extends EntityA {
		static final String INDEX = "1A";

		public Entity1A() {
		}

		public Entity1A(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}

	@Entity
	@Indexed(index = Entity1B.INDEX)
	public static class Entity1B extends EntityA {
		static final String INDEX = "1B";

		public Entity1B() {
		}

		public Entity1B(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}

	@Entity
	@Indexed(index = Entity2A.INDEX)
	public static class Entity2A extends Entity1A {
		static final String INDEX = "2A";

		public Entity2A() {
		}

		public Entity2A(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}
}
