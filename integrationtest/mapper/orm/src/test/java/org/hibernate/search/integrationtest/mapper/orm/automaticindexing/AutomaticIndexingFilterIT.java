/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class AutomaticIndexingFilterIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
						.field( "indexedField", String.class )
						.objectField(
								"containedIndexedEmbedded", b2 -> b2.field( "indexedField", String.class ).multiValued( true )
						)
				)
				.expectSchema( EntityA.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity2A.INDEX, b -> b.field( "indexedField", String.class ) )
				.expectSchema( Entity1B.INDEX, b -> b.field( "indexedField", String.class ) );

		setupContext.withAnnotatedTypes( IndexedEntity.class, ContainedEntity.class,
				EntityA.class, Entity1A.class, Entity1B.class, Entity2A.class
		);
	}

	@Before
	public void clearFilter() throws Exception {
		Search.automaticIndexingFilter(
				setupHolder.entityManagerFactory(),
				ctx -> { /*clear out any settings from tests*/ }
		);
	}

	@Test
	public void directPersistUpdateDelete() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );

			ContainedEntity entity2 = new ContainedEntity();
			entity2.setId( 100 );
			entity2.setIndexedField( "initialValue" );

			entity2.setContainingAsIndexedEmbedded( entity1 );
			entity1.setContainedIndexedEmbedded( Arrays.asList( entity2 ) );

			session.persist( entity1 );
			session.persist( entity2 );

		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedField( "updatedValue" );

		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			entity1.getContainedIndexedEmbedded().forEach( e -> e.setContainingAsIndexedEmbedded( null ) );

			session.remove( entity1 );

		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directPersistUpdateDeleteApplicationFilter() {
		Search.automaticIndexingFilter(
				setupHolder.entityManagerFactory(),
				ctx -> ctx.exclude( IndexedEntity.class )
		);
		setupHolder.runInTransaction( session -> {

			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );

			ContainedEntity entity2 = new ContainedEntity();
			entity2.setId( 100 );
			entity2.setIndexedField( "initialValue" );

			entity2.setContainingAsIndexedEmbedded( entity1 );
			entity1.setContainedIndexedEmbedded( Arrays.asList( entity2 ) );

			session.persist( entity1 );
			session.persist( entity2 );

		} );
		backendMock.verifyExpectationsMet();

		Search.automaticIndexingFilter(
				setupHolder.entityManagerFactory(),
				ctx -> ctx.exclude( IndexedEntity.class )
		);
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedField( "updatedValue" );

		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			entity1.getContainedIndexedEmbedded().forEach( e -> e.setContainingAsIndexedEmbedded( null ) );

			session.remove( entity1 );

		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void hierarchyFiltering() {
		// exclude all except one specific class.
		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.exclude( EntityA.class )
					.include( Entity2A.class ) );

			session.persist( new EntityA( 1, "test" ) );
			session.persist( new Entity1A( 2, "test" ) );
			session.persist( new Entity1B( 3, "test" ) );
			session.persist( new Entity2A( 4, "test" ) );

			backendMock.expectWorks( Entity2A.INDEX )
					.add( "4", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();

		// exclude all except one class branch.
		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.exclude( EntityA.class )
					.include( Entity1A.class ) );

			session.persist( new EntityA( 10, "test" ) );
			session.persist( new Entity1A( 20, "test" ) );
			session.persist( new Entity1B( 30, "test" ) );
			session.persist( new Entity2A( 40, "test" ) );

			backendMock.expectWorks( Entity1A.INDEX )
					.add( "20", b -> b.field( "indexedField", "test" ) );
			backendMock.expectWorks( Entity2A.INDEX )
					.add( "40", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();

		// only include - should include all since no excludes.
		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.include( Entity1A.class ) );

			session.persist( new EntityA( 100, "test" ) );
			session.persist( new Entity1A( 200, "test" ) );
			session.persist( new Entity1B( 300, "test" ) );
			session.persist( new Entity2A( 400, "test" ) );

			backendMock.expectWorks( EntityA.INDEX )
					.add( "100", b -> b.field( "indexedField", "test" ) );
			backendMock.expectWorks( Entity1A.INDEX )
					.add( "200", b -> b.field( "indexedField", "test" ) );
			backendMock.expectWorks( Entity1B.INDEX )
					.add( "300", b -> b.field( "indexedField", "test" ) );
			backendMock.expectWorks( Entity2A.INDEX )
					.add( "400", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void sameClassFails() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).automaticIndexingFilter( ctx -> ctx.exclude( EntityA.class )
							.include( EntityA.class ) )
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							EntityA.class.getName(),
							"cannot be included and excluded at the same time within one filter",
							"Already included types: '[]'",
							"Already excluded types:"
					);

			assertThatThrownBy( () ->
					Search.session( session ).automaticIndexingFilter( ctx -> ctx.include( EntityA.class )
							.exclude( EntityA.class ) )
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							EntityA.class.getName(),
							"cannot be included and excluded at the same time within one filter",
							"Already included types:",
							"Already excluded types: '[]'"
					);
		} );
	}

	@Test
	public void applicationFilterDisableAll() {
		Search.automaticIndexingFilter(
				setupHolder.entityManagerFactory(),
				ctx -> ctx.exclude( EntityA.class )
		);
		setupHolder.runInTransaction( session -> {
			session.persist( new EntityA( 1, "test" ) );
			session.persist( new Entity1A( 2, "test" ) );
			session.persist( new Entity1B( 3, "test" ) );
			session.persist( new Entity2A( 4, "test" ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.include( Entity2A.class ) );

			session.persist( new EntityA( 10, "test" ) );
			session.persist( new Entity1A( 20, "test" ) );
			session.persist( new Entity1B( 30, "test" ) );
			session.persist( new Entity2A( 40, "test" ) );

			backendMock.expectWorks( Entity2A.INDEX )
					.add( "40", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void applicationFilterOnly() {
		Search.automaticIndexingFilter(
				setupHolder.entityManagerFactory(),
				ctx -> ctx.exclude( EntityA.class )
						.include( Entity2A.class )
		);

		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.include( Entity2A.class ) );

			session.persist( new EntityA( 10, "test" ) );
			session.persist( new Entity1A( 20, "test" ) );
			session.persist( new Entity1B( 30, "test" ) );
			session.persist( new Entity2A( 40, "test" ) );

			backendMock.expectWorks( Entity2A.INDEX )
					.add( "40", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void applicationFilterExcludeSessionInclude() {
		Search.automaticIndexingFilter(
				setupHolder.entityManagerFactory(),
				ctx -> ctx.exclude( Entity2A.class )
		);

		setupHolder.runInTransaction( session -> {
			Search.session( session ).automaticIndexingFilter( ctx -> ctx.include( Entity2A.class ) );

			session.persist( new Entity2A( 40, "test" ) );

			backendMock.expectWorks( Entity2A.INDEX )
					.add( "40", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "containing")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		@Basic
		private String nonIndexedField;

		@OneToMany(mappedBy = "containingAsIndexedEmbedded")
		@IndexedEmbedded
		private Collection<ContainedEntity> containedIndexedEmbedded = new ArrayList<>();

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
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

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public Collection<ContainedEntity> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Collection<ContainedEntity> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		@ManyToOne
		private IndexedEntity containingAsIndexedEmbedded;

		@Basic
		@GenericField
		private String indexedField;


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public void setContainingAsIndexedEmbedded(IndexedEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
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
