/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmIndexingPlanFilterIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		this.entityManagerFactory = setupHelper.start()
				.setup( EntityA.class, EntityExtendsA1.class, EntityExtendsA2.class );
	}

	@Test
	public void applicationFilterOnly() {
		// tag::application-filter[]
		Search.mapping( entityManagerFactory ).indexingPlanFilter( // <1>
				ctx -> ctx.exclude( EntityA.class ) // <2>
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
	public void sessionFilterOnly() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::session-filter[]
			Search.session( entityManager ) // <1>
					.automaticIndexingFilter( // <2>
							ctx -> ctx.exclude( EntityA.class )
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
