/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Arrays;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

public class ApplicationIndexingPlanFilterIT extends AbstractIndexingPlanFilterIT {

	@Test
	public void directPersistUpdateDeleteApplicationFilter() {
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
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

		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
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
	public void directPersistUpdateDeleteApplicationFilterByName() {
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( IndexedEntity.INDEX )
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

		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( IndexedEntity.INDEX )
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
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( EntityA.class )
						.include( Entity2A.class )
		);

		setupHolder.runInTransaction( session -> {
			session.persist( new EntityA( 1, "test" ) );
			session.persist( new Entity1A( 2, "test" ) );
			session.persist( new Entity1B( 3, "test" ) );
			session.persist( new Entity2A( 4, "test" ) );

			backendMock.expectWorks( Entity2A.INDEX )
					.add( "4", b -> b.field( "indexedField", "test" ) );
		} );
		backendMock.verifyExpectationsMet();

		// exclude all except one class branch.
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( EntityA.class )
						.include( Entity1A.class )
		);
		setupHolder.runInTransaction( session -> {
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
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.include( Entity1A.class )
		);
		setupHolder.runInTransaction( session -> {
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
		assertThatThrownBy( () -> Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( EntityA.class )
						.include( EntityA.class )
		)
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						EntityA.class.getName(),
						"cannot be included and excluded at the same time within one filter",
						"Already included types: '[]'",
						"Already excluded types:"
				);

		assertThatThrownBy( () -> Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.include( EntityA.class )
						.exclude( EntityA.class )
		)
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						EntityA.class.getName(),
						"cannot be included and excluded at the same time within one filter",
						"Already included types:",
						"Already excluded types: '[]'"
				);
	}

	@Test
	public void sameNameFails() {
		assertThatThrownBy( () -> Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
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
	}

	@Test
	public void applicationFilterDisableAllByName() {
		Search.mapping( setupHolder.entityManagerFactory() ).indexingPlanFilter(
				ctx -> ctx.exclude( EntityA.INDEX )
		);
		setupHolder.runInTransaction( session -> {
			session.persist( new EntityA( 1, "test" ) );
			session.persist( new Entity1A( 2, "test" ) );
			session.persist( new Entity1B( 3, "test" ) );
			session.persist( new Entity2A( 4, "test" ) );
		} );
		backendMock.verifyExpectationsMet();
	}
}
