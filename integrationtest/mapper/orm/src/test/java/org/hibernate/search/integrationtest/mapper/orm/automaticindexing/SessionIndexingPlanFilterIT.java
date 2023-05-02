/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

public class SessionIndexingPlanFilterIT extends AbstractIndexingPlanFilterIT {

	@Test
	public void directPersistUpdateDelete() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

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
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedField( "updatedValue" );

		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			entity1.getContainedIndexedEmbedded().forEach( e -> e.setContainingAsIndexedEmbedded( null ) );

			session.remove( entity1 );

		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directPersistUpdateDeleteOfNotDisabledEntity() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			IndexedEntity entity0 = new IndexedEntity();
			entity0.setId( 1 );
			entity0.setIndexedField( "initialValue" );

			OtherIndexedEntity entity1 = new OtherIndexedEntity();
			entity1.setId( 10 );
			entity1.setIndexedField( "initialValue" );

			ContainedEntity entity2 = new ContainedEntity();
			entity2.setId( 100 );
			entity2.setIndexedField( "initialValue" );


			entity2.setContainingAsIndexedEmbedded( entity0 );
			entity0.setContainedIndexedEmbedded( Arrays.asList( entity2 ) );
			entity2.setOtherContainingAsIndexedEmbedded( entity1 );
			entity1.setContainedIndexedEmbedded( Arrays.asList( entity2 ) );

			session.persist( entity0 );
			session.persist( entity1 );
			session.persist( entity2 );

			backendMock.expectWorks( OtherIndexedEntity.INDEX )
					.add( "10", b -> b.field( "indexedField", "initialValue" )
							.objectField(
									"containedIndexedEmbedded", b2 -> b2.field( "indexedField", "initialValue" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			OtherIndexedEntity entity1 = session.get( OtherIndexedEntity.class, 10 );
			entity1.setIndexedField( "updatedValue" );

			backendMock.expectWorks( OtherIndexedEntity.INDEX )
					.addOrUpdate( "10", b -> b.field( "indexedField", "updatedValue" )
							.objectField(
									"containedIndexedEmbedded", b2 -> b2.field( "indexedField", "initialValue" ) ) );

		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( IndexedEntity.class ) );

			IndexedEntity entity0 = session.get( IndexedEntity.class, 1 );
			OtherIndexedEntity entity1 = session.get( OtherIndexedEntity.class, 10 );

			entity0.getContainedIndexedEmbedded().forEach( e -> e.setContainingAsIndexedEmbedded( null ) );
			entity1.getContainedIndexedEmbedded().forEach( e -> e.setOtherContainingAsIndexedEmbedded( null ) );

			session.remove( entity0 );
			session.remove( entity1 );

			backendMock.expectWorks( OtherIndexedEntity.INDEX )
					.delete( "10" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void hierarchyFiltering() {
		// exclude all except one specific class.
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( EntityA.class )
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
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( EntityA.class )
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
			Search.session( session ).indexingPlanFilter( ctx -> ctx.include( Entity1A.class ) );

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
					Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( EntityA.class )
							.include( EntityA.class ) )
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							EntityA.class.getName(),
							"cannot be included and excluded at the same time within one filter",
							"Already included types: '[]'",
							"Already excluded types:"
					);

			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter( ctx -> ctx.include( EntityA.class )
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
	public void sameNameFails() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.include( EntityA.INDEX )
									.exclude( EntityA.INDEX )
					)
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
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
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
			Search.session( session ).indexingPlanFilter( ctx -> ctx.include( Entity2A.class ) );

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
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( Entity2A.class )
		);

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.include( Entity2A.class ) );

			session.persist( new Entity2A( 40, "test" ) );

			backendMock.expectWorks( Entity2A.INDEX )
					.add( "40", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void applicationFilterIncludeOneSubtypeSessionIncludesAnother() {
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( EntityA.class )
						.include( Entity1B.class )
		);

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.include( Entity2A.class ) );

			session.persist( new Entity1B( 30, "test" ) );
			session.persist( new Entity2A( 40, "test" ) );

			backendMock.expectWorks( Entity1B.INDEX )
					.add( "30", b -> b.field( "indexedField", "test" ) );
			backendMock.expectWorks( Entity2A.INDEX )
					.add( "40", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void filterByMappedSuperclass() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( SuperClass.class ) );

			session.persist( new EntityFromSuperclass( 100, "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void filterByNotIndexedEntity() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( SimpleNotIndexedEntity.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							SimpleNotIndexedEntity.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type"
					);
		} );
	}

	@Test
	public void filterByRandomClass() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( NotAnEntity.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							NotAnEntity.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type"
					);
		} );
	}

	@Test
	public void filterByNotIndexedEntityFormSupertypeWithIndexedSubtype() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( NotIndexedEntityFromSuperclass.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							NotIndexedEntityFromSuperclass.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type"
					);
		} );
	}

	@Test
	public void filterByIndexedTypeNotAnEntity() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( IndexedNotAnEntity.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							IndexedNotAnEntity.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type"
					);
		} );
	}

	@Test
	public void filterByIntegerShouldFail() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.include( Integer.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							Integer.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type"
					);
		} );
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( Integer.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							Integer.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type"
					);
		} );
	}

	@Test
	public void filterBySomeString() {
		setupHolder.runInTransaction( session -> {
			String name = "this is not a name that should work";
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( name )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for the name",
							name,
							"This name represents neither an entity type mapped in Hibernate Search nor a superclass of such entity type",
							"Valid entity type names are"
					);
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.include( name )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for the name",
							name,
							"This name represents neither an entity type mapped in Hibernate Search nor a superclass of such entity type",
							"Valid entity type names are"
					);
		} );
	}

	@Test
	public void filterByContainedEntityWontAffectContainingOnes() {
		setupHolder.runInTransaction( session -> {
			// to prepare data we ignore containing/indexed entity
			Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( IndexedEntity.class )
			);

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
			// now disable contained entity to not produce updates on containing:
			Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( ContainedEntity.class )
			);
			ContainedEntity entity1 = session.get( ContainedEntity.class, 100 );
			entity1.setIndexedField( "updatedValue" );
		} );

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter(
					ctx -> ctx.exclude( IndexedEntity.class )
			);
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			entity1.getContainedIndexedEmbedded().forEach( e -> e.setContainingAsIndexedEmbedded( null ) );

			session.remove( entity1 );

		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void filterByInterfaceMustFail() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( InterfaceA.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							InterfaceA.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type."
					);
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.include( InterfaceA.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							InterfaceA.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type."
					);
		} );
	}

	@Test
	public void excludeByNameOfNotIndexedSupertype() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( NotIndexedEntity.NAME ) );

			session.persist( new IndexedSubtypeOfNotIndexedEntity( 40, "test", "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	// Maps are used to represent dynamic types and we want to make sure that passing a Map won't work.
	@Test
	public void filterByMapInterfaceMustFail() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( Map.class )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for class",
							Map.class.getName(),
							"This class is neither an entity type mapped in Hibernate Search nor a superclass of such entity type."
					);
		} );
	}

	@Test
	public void dynamicTypeByNameDirectPersistUpdateDelete() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( DYNAMIC_BASE_TYPE_A ) );

			Map<String, Object> entity1 = new HashMap<>();
			entity1.put( "id", 1 );
			entity1.put( "propertyOfA", "string1" );
			entity1.put( "propertyOfB", 1 );

			Map<String, Object> entity2 = new HashMap<>();
			entity2.put( "id", 2 );
			entity2.put( "propertyOfA", "string2" );
			entity2.put( "propertyOfC", LocalDate.of( 2023, Month.MAY, 2 ) );

			session.persist( DYNAMIC_SUBTYPE_B, entity1 );
			session.persist( DYNAMIC_SUBTYPE_C, entity2 );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( DYNAMIC_BASE_TYPE_A ) );

			@SuppressWarnings("unchecked")
			Map<String, Object> entity1 = (Map<String, Object>) session.get( DYNAMIC_SUBTYPE_B, 1 );
			entity1.put( "propertyOfA", "updatedValue" );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( DYNAMIC_BASE_TYPE_A ) );

			session.remove( session.get( DYNAMIC_SUBTYPE_B, 1 ) );
			session.remove( session.get( DYNAMIC_SUBTYPE_C, 2 ) );

		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void dynamicTypeByNameApplicationDisableAllSessionEnableSubtype() {
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( DYNAMIC_BASE_TYPE_A )
		);
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.include( DYNAMIC_SUBTYPE_B ) );

			Map<String, Object> entity1 = new HashMap<>();
			entity1.put( "id", 1 );
			entity1.put( "propertyOfA", "string1" );
			entity1.put( "propertyOfB", 1 );

			Map<String, Object> entity2 = new HashMap<>();
			entity2.put( "id", 2 );
			entity2.put( "propertyOfA", "string2" );
			entity2.put( "propertyOfC", LocalDate.of( 2023, Month.MAY, 2 ) );

			session.persist( DYNAMIC_SUBTYPE_B, entity1 );
			session.persist( DYNAMIC_SUBTYPE_C, entity2 );

			backendMock.expectWorks( DYNAMIC_SUBTYPE_B )
					.add( "1", b -> b.field( "propertyOfA", "string1" )
							.field( "propertyOfB", 1 ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void excludeByNameOfNotIndexedSupertypeDynamicTypes() {
		setupHolder.runInTransaction( session -> {
			Search.session( session ).indexingPlanFilter( ctx -> ctx.exclude( DYNAMIC_NOT_INDEXED_BASE_TYPE_A ) );

			Map<String, Object> entity1 = new HashMap<>();
			entity1.put( "id", 1 );
			entity1.put( "propertyOfB", 1 );

			session.persist( DYNAMIC_INDEXED_SUBTYPE_A_B, entity1 );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void excludeByNameOfNotIndexedSupertypeThatHasNoIndexedOrContainedSubtypesDynamicTypes() {
		setupHolder.runInTransaction( session -> {
			assertThatThrownBy( () ->
					Search.session( session ).indexingPlanFilter(
							ctx -> ctx.exclude( DYNAMIC_NOT_INDEXED_BASE_TYPE_B )
					)
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"No matching entity type for the name",
							DYNAMIC_NOT_INDEXED_BASE_TYPE_B,
							"This name represents neither an entity type mapped in Hibernate Search nor a superclass of such entity type."
					);
		} );
		backendMock.verifyExpectationsMet();
	}
}
