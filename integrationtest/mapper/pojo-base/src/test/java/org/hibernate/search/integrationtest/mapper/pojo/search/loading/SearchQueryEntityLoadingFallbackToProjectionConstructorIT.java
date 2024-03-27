/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.data.Pair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test the fallback to projection constructors for the "default projection" (no select() call)
 * when no loader is registered for the loaded type.
 */
@TestForIssue(jiraKey = "HSEARCH-4579")
class SearchQueryEntityLoadingFallbackToProjectionConstructorIT {

	private static final String ENTITY_NAME = "entity_name";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	protected final StubLoadingContext loadingContext = new StubLoadingContext();

	@Test
	void withoutLoadingStrategy_withoutProjectionConstructor() {
		@SearchEntity(name = ENTITY_NAME)
		@Indexed
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( ENTITY_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSessionWithOptions()
				.loading( o -> o.context( StubLoadingContext.class, loadingContext ) )
				.build() ) {
			backendMock.expectSearchProjection(
					ENTITY_NAME,
					// The result doesn't matter, we should get a failure as soon as there's at least one hit
					StubSearchWorkBehavior.of(
							1,
							reference( ENTITY_NAME, "1" )
					)
			);

			assertThatThrownBy( () -> session.search( IndexedEntity.class ).where( f -> f.matchAll() ).fetchAllHits() )
					.hasMessageContainingAll(
							"Cannot project on entity type '" + ENTITY_NAME
									+ "': this type cannot be loaded from an external datasource,"
									+ " and the documents from the index cannot be projected to its Java class '"
									+ IndexedEntity.class.getName() + "'",
							"To enable loading of entity instances from an external source, provide a SelectionLoadingStrategy"
									+ " when registering the entity type to the mapping builder",
							"To enable projections turning taking index data into entity instances,"
									+ " annotate a constructor of the entity type with @ProjectionConstructor",
							"See the reference documentation for more information."
					);
		}
	}

	@Test
	void withoutLoadingStrategy_withProjectionConstructor() {
		@SearchEntity(name = ENTITY_NAME)
		@Indexed
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;

			@ProjectionConstructor
			public IndexedEntity(@IdProjection Integer id, String text) {
				this.id = id;
				this.text = text;
			}
		}

		backendMock.expectAnySchema( ENTITY_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
		backendMock.verifyExpectationsMet();

		IndexedEntity instance1 = new IndexedEntity( 1, "text1" );
		IndexedEntity instance2 = new IndexedEntity( 2, "text2" );
		IndexedEntity instance3 = new IndexedEntity( 3, "text3" );

		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchProjection(
					ENTITY_NAME,
					StubSearchWorkBehavior.of(
							3,
							Arrays.asList( String.valueOf( instance1.id ), instance1.text ),
							Arrays.asList( String.valueOf( instance2.id ), instance2.text ),
							Arrays.asList( String.valueOf( instance3.id ), instance3.text )
					)
			);

			assertThat( session.search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveComparison()
					.isEqualTo( Arrays.asList( instance1, instance2, instance3 ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void withLoadingStrategy_withProjectionConstructor() {
		backendMock.expectAnySchema( ENTITY_NAME );
		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.setup( IndexedEntityWithLoadingStrategyAndProjectionConstructor.class );
		backendMock.verifyExpectationsMet();

		IndexedEntityWithLoadingStrategyAndProjectionConstructor instance1 =
				new IndexedEntityWithLoadingStrategyAndProjectionConstructor( 1, "text1" );
		IndexedEntityWithLoadingStrategyAndProjectionConstructor instance2 =
				new IndexedEntityWithLoadingStrategyAndProjectionConstructor( 2, "text2" );
		IndexedEntityWithLoadingStrategyAndProjectionConstructor instance3 =
				new IndexedEntityWithLoadingStrategyAndProjectionConstructor( 3, "text3" );

		loadingContext.persistenceMap( IndexedEntityWithLoadingStrategyAndProjectionConstructor.PERSISTENCE_KEY )
				.put( instance1.id, instance1 );
		loadingContext.persistenceMap( IndexedEntityWithLoadingStrategyAndProjectionConstructor.PERSISTENCE_KEY )
				.put( instance2.id, instance2 );
		loadingContext.persistenceMap( IndexedEntityWithLoadingStrategyAndProjectionConstructor.PERSISTENCE_KEY )
				.put( instance3.id, instance3 );

		try ( SearchSession session = mapping.createSessionWithOptions()
				.loading( o -> o.context( StubLoadingContext.class, loadingContext ) )
				.build() ) {
			backendMock.expectSearchProjection(
					ENTITY_NAME,
					StubSearchWorkBehavior.of(
							3,
							reference( ENTITY_NAME, String.valueOf( instance1.id ) ),
							reference( ENTITY_NAME, String.valueOf( instance2.id ) ),
							reference( ENTITY_NAME, String.valueOf( instance3.id ) )
					)
			);

			assertThat( session.search( IndexedEntityWithLoadingStrategyAndProjectionConstructor.class )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					// The loading strategy gets precedence over the projection constructor,
					// so expect entities to be loaded.
					// Using identity comparison for elements:
					// we really expect the exact same instances,
					// because that's how our (stub) loading strategy works.
					.isEqualTo( Arrays.asList(
							instance1,
							instance2,
							instance3
					) );
			assertThat( loadingContext.loaderCalls() )
					.hasSize( 1 )
					.element( 0 )
					.satisfies( call -> assertThat( call.ids )
							.containsExactlyInAnyOrder( instance1.id, instance2.id, instance3.id ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@SearchEntity(name = ENTITY_NAME,
			loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	@Indexed
	public static class IndexedEntityWithLoadingStrategyAndProjectionConstructor {
		public static final PersistenceTypeKey<IndexedEntityWithLoadingStrategyAndProjectionConstructor,
				Integer> PERSISTENCE_KEY =
						new PersistenceTypeKey<>( IndexedEntityWithLoadingStrategyAndProjectionConstructor.class,
								Integer.class );
		@DocumentId
		public Integer id;
		@FullTextField
		public String text;

		@ProjectionConstructor
		public IndexedEntityWithLoadingStrategyAndProjectionConstructor(@IdProjection Integer id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Test
	void multiType() {
		backendMock.expectAnySchema( MultiTypeModel.IndexedEntityWithLoadingStrategy.NAME );
		backendMock.expectAnySchema( MultiTypeModel.IndexedEntityWithLoadingStrategyChild.NAME );
		backendMock.expectAnySchema( MultiTypeModel.IndexedEntityWithProjectionConstructor.NAME );
		backendMock.expectAnySchema( MultiTypeModel.IndexedEntityWithProjectionConstructorChild.NAME );
		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes(
						MultiTypeModel.IndexedEntityWithLoadingStrategy.class,
						MultiTypeModel.IndexedEntityWithLoadingStrategyChild.class,
						MultiTypeModel.IndexedEntityWithProjectionConstructor.class,
						MultiTypeModel.IndexedEntityWithProjectionConstructorChild.class
				)
				.setup();
		backendMock.verifyExpectationsMet();


		MultiTypeModel.IndexedEntityWithLoadingStrategy withLoadingStrategyInstance =
				new MultiTypeModel.IndexedEntityWithLoadingStrategy( 1, "text1" );
		MultiTypeModel.IndexedEntityWithLoadingStrategyChild withLoadingStrategyChildInstance =
				new MultiTypeModel.IndexedEntityWithLoadingStrategyChild( 2, "text2" );
		MultiTypeModel.IndexedEntityWithProjectionConstructor withProjectionConstructorInstance =
				new MultiTypeModel.IndexedEntityWithProjectionConstructor( 3, "text3" );
		MultiTypeModel.IndexedEntityWithProjectionConstructorChild withProjectionConstructorChildInstance =
				new MultiTypeModel.IndexedEntityWithProjectionConstructorChild( 3, "text3" );

		loadingContext.persistenceMap( MultiTypeModel.IndexedEntityWithLoadingStrategy.PERSISTENCE_KEY )
				.put( withLoadingStrategyInstance.id, withLoadingStrategyInstance );
		loadingContext.persistenceMap( MultiTypeModel.IndexedEntityWithLoadingStrategy.PERSISTENCE_KEY )
				.put( withLoadingStrategyChildInstance.id, withLoadingStrategyChildInstance );
		loadingContext.persistenceMap( MultiTypeModel.IndexedEntityWithProjectionConstructorChild.PERSISTENCE_KEY )
				.put( withProjectionConstructorChildInstance.id, withProjectionConstructorChildInstance );

		try ( SearchSession session = mapping.createSessionWithOptions()
				.loading( o -> o.context( StubLoadingContext.class, loadingContext ) )
				.build() ) {
			backendMock.expectSearchProjection(
					Arrays.asList( MultiTypeModel.IndexedEntityWithLoadingStrategy.NAME,
							MultiTypeModel.IndexedEntityWithLoadingStrategyChild.NAME,
							MultiTypeModel.IndexedEntityWithProjectionConstructor.NAME,
							MultiTypeModel.IndexedEntityWithProjectionConstructorChild.NAME ),
					StubSearchWorkBehavior.of(
							4,
							reference( MultiTypeModel.IndexedEntityWithLoadingStrategy.NAME,
									String.valueOf( withLoadingStrategyInstance.id ) ),
							// The loading strategy takes precedence over the projection constructor,
							// so expect IndexedEntityWithLoadingStrategyChild to be loaded.
							reference( MultiTypeModel.IndexedEntityWithLoadingStrategyChild.NAME,
									String.valueOf( withLoadingStrategyChildInstance.id ) ),
							new Pair<>( MultiTypeModel.IndexedEntityWithProjectionConstructor.NAME,
									Arrays.asList( String.valueOf( withProjectionConstructorInstance.id ),
											withProjectionConstructorInstance.text ) ),
							// The loading strategy takes precedence over the projection constructor,
							// so expect IndexedEntityWithProjectionConstructorChild to be loaded.
							reference( MultiTypeModel.IndexedEntityWithProjectionConstructorChild.NAME,
									String.valueOf( withProjectionConstructorChildInstance.id ) )
					)
			);

			assertThat( session.search( Arrays.asList( MultiTypeModel.IndexedEntityWithLoadingStrategy.class,
					MultiTypeModel.IndexedEntityWithProjectionConstructor.class
			) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveComparison()
					.isEqualTo( Arrays.asList(
							withLoadingStrategyInstance,
							withLoadingStrategyChildInstance,
							withProjectionConstructorInstance,
							withProjectionConstructorChildInstance
					) );

			// Three out of the four types should actually rely on loading;
			// the one entity without a loading strategy relies on projection constructors.
			assertThat( loadingContext.loaderCalls() )
					.satisfiesExactlyInAnyOrder(
							c -> assertThat( c.ids ).containsExactlyInAnyOrder( withLoadingStrategyInstance.id,
									withLoadingStrategyChildInstance.id ),
							c -> assertThat( c.ids ).containsExactlyInAnyOrder(
									withProjectionConstructorChildInstance.id )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	public static class MultiTypeModel {
		@SearchEntity(name = IndexedEntityWithLoadingStrategy.NAME,
				loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
		@Indexed
		public static class IndexedEntityWithLoadingStrategy {
			public static final String NAME = "WithLoad";
			public static final PersistenceTypeKey<IndexedEntityWithLoadingStrategy, Integer> PERSISTENCE_KEY =
					new PersistenceTypeKey<>( IndexedEntityWithLoadingStrategy.class, Integer.class );

			@DocumentId
			@GenericField
			public Integer id;
			@FullTextField
			public String text;

			public IndexedEntityWithLoadingStrategy(Integer id, String text) {
				this.id = id;
				this.text = text;
			}
		}

		@SearchEntity(name = IndexedEntityWithLoadingStrategyChild.NAME)
		@Indexed
		public static class IndexedEntityWithLoadingStrategyChild extends IndexedEntityWithLoadingStrategy {
			public static final String NAME = "WithLoadChild";

			// A projection constructor unlike the parent class, but the loading strategy is inherited
			@ProjectionConstructor
			public IndexedEntityWithLoadingStrategyChild(@IdProjection Integer id, String text) {
				super( id, text );
			}
		}

		@SearchEntity(name = IndexedEntityWithProjectionConstructor.NAME)
		@Indexed
		public static class IndexedEntityWithProjectionConstructor {
			public static final String NAME = "WithProj";

			@DocumentId
			public Integer id;
			@FullTextField
			public String text;

			@ProjectionConstructor
			public IndexedEntityWithProjectionConstructor(@IdProjection Integer id, String text) {
				this.id = id;
				this.text = text;
			}
		}

		@SearchEntity(name = IndexedEntityWithProjectionConstructorChild.NAME,
				loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
		@Indexed
		public static class IndexedEntityWithProjectionConstructorChild extends IndexedEntityWithProjectionConstructor {
			public static final String NAME = "WithProjChild";
			public static final PersistenceTypeKey<IndexedEntityWithProjectionConstructorChild, Integer> PERSISTENCE_KEY =
					new PersistenceTypeKey<>( IndexedEntityWithProjectionConstructorChild.class, Integer.class );


			// No projection constructor unlike the parent class, but there is a loading strategy
			public IndexedEntityWithProjectionConstructorChild(Integer id, String text) {
				super( id, text );
			}
		}
	}
}
