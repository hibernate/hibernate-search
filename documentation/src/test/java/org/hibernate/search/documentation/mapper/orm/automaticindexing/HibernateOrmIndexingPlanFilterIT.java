/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HibernateOrmIndexingPlanFilterIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		this.entityManagerFactory = setupHelper.start()
				.setup( EntityA.class, EntityExtendsA1.class, EntityExtendsA2.class );
	}

	@Test
	void applicationFilterOnly() {
		// tag::application-filter[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::application-filter[]
				Search.mapping( entityManagerFactory );
		// tag::application-filter[]
		searchMapping.indexingPlanFilter( // <2>
				ctx -> ctx.exclude( EntityA.class ) // <3>
						.include( EntityExtendsA2.class )
		);
		// end::application-filter[]
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			entityManager.persist( new EntityA( 10, "test" ) );
			entityManager.persist( new EntityExtendsA1( 20, "test" ) );
			entityManager.persist( new EntityExtendsA2( 30, "test" ) );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// Only EntityExtendsA2 is getting indexed:
			assertThat( Search.session( entityManager ).search( EntityA.class )
					.select( f -> f.id() )
					.where( f -> f.matchAll() )
					.fetchAllHits()
			).containsOnly( 30 );
		} );
	}

	@Test
	void sessionFilterOnly() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::session-filter[]
			SearchSession session = /* ... */ // <1>
					// end::session-filter[]
					Search.session( entityManager );
			// tag::session-filter[]
			session.indexingPlanFilter(
					ctx -> ctx.exclude( EntityA.class ) // <2>
							.include( EntityExtendsA2.class )
			);
			// end::session-filter[]

			entityManager.persist( new EntityA( 10, "test" ) );
			entityManager.persist( new EntityExtendsA1( 20, "test" ) );
			entityManager.persist( new EntityExtendsA2( 30, "test" ) );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// Only EntityExtendsA2 is getting indexed:
			assertThat( Search.session( entityManager ).search( EntityA.class )
					.select( f -> f.id() )
					.where( f -> f.matchAll() )
					.fetchAllHits()
			).containsOnly( 30 );
		} );
	}

	@Test
	void disableAll() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::session-filter-exclude-all[]
			SearchSession searchSession = /* ... */ // <1>
					// end::session-filter-exclude-all[]
					Search.session( entityManager );
			// tag::session-filter-exclude-all[]
			searchSession.indexingPlanFilter(
					ctx -> ctx.exclude( Object.class ) // <2>
			);
			// end::session-filter-exclude-all[]

			entityManager.persist( new EntityA( 10, "test" ) );
			entityManager.persist( new EntityExtendsA1( 20, "test" ) );
			entityManager.persist( new EntityExtendsA2( 30, "test" ) );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// Nothing is getting indexed:
			assertThat( Search.session( entityManager ).search( EntityA.class )
					.select( f -> f.id() )
					.where( f -> f.matchAll() )
					.fetchAllHits()
			).isEmpty();
		} );
	}

	@Test
	void disableAllApplicationEnableSession() {
		// tag::session-filter-exclude-include-all-application[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::session-filter-exclude-include-all-application[]
				Search.mapping( entityManagerFactory );
		// tag::session-filter-exclude-include-all-application[]
		searchMapping.indexingPlanFilter(
				ctx -> ctx.exclude( Object.class ) // <2>
		);
		// end::session-filter-exclude-include-all-application[]
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::session-filter-exclude-include-all-session[]
			SearchSession searchSession = /* ... */ // <3>
					// end::session-filter-exclude-include-all-session[]
					Search.session( entityManager );
			// tag::session-filter-exclude-include-all-session[]
			searchSession.indexingPlanFilter(
					ctx -> ctx.include( Object.class ) // <4>
			);
			// end::session-filter-exclude-include-all-session[]

			entityManager.persist( new EntityA( 10, "test" ) );
			entityManager.persist( new EntityExtendsA1( 20, "test" ) );
			entityManager.persist( new EntityExtendsA2( 30, "test" ) );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// Nothing is getting indexed:
			assertThat( Search.session( entityManager ).search( EntityA.class )
					.select( f -> f.id() )
					.where( f -> f.matchAll() )
					.fetchAllHits()
			).containsOnly(
					10, 20, 30
			);
		} );
	}

	@Entity(name = EntityA.INDEX)
	@Indexed
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

	@Entity(name = EntityExtendsA1.INDEX)
	@Indexed
	public static class EntityExtendsA1 extends EntityA {
		static final String INDEX = "A1";

		public EntityExtendsA1() {
		}

		public EntityExtendsA1(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}

	@Entity(name = EntityExtendsA2.INDEX)
	@Indexed
	public static class EntityExtendsA2 extends EntityA {
		static final String INDEX = "B1";

		public EntityExtendsA2() {
		}

		public EntityExtendsA2(Integer id, String indexedField) {
			super( id, indexedField );
		}
	}
}
