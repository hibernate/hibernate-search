/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class OutboxPollingIndexingPlanFilterIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock
				.expectSchema( EntityA.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity2A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1B.INDEX, b -> b.field( "indexedField", String.class ) );

		setupContext.withAnnotatedTypes(
				EntityA.class, Entity1A.class, Entity1B.class, Entity2A.class
		);
	}

	@Before
	public void clearFilter() throws Exception {
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> { /*clear out any settings from tests*/ }
		);
	}

	@Test
	public void partialSessionFilterFails() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( Entity1A.class ) )
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Unable to apply the given filter at the session level with the outbox polling coordination strategy.",
							"With this coordination strategy, applying a session-level indexing plan filter is only allowed if it excludes all types."
					);
		} );
	}

	@Test
	public void allTypesMixSessionFilterFails() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
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
	public void allTypesExcludeExplicitlySessionFilter() {
		setupHolder.runInTransaction( session -> {
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
	public void allTypesExcludeInheritanceSessionFilter() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( EntityA.class )
			);

			session.persist( new EntityA( 1, "test" ) );
			session.persist( new Entity1A( 2, "test" ) );
			session.persist( new Entity1B( 3, "test" ) );
			session.persist( new Entity2A( 4, "test" ) );
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
