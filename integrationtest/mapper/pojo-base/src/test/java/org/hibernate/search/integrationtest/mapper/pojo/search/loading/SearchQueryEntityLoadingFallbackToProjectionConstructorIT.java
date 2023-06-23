/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.data.Pair;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the fallback to projection constructors for the "default projection" (no select() call)
 * when no loader is registered for the loaded type.
 */
@TestForIssue(jiraKey = "HSEARCH-4579")
public class SearchQueryEntityLoadingFallbackToProjectionConstructorIT {

	private static final String ENTITY_NAME = "entity_name";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	protected final StubLoadingContext loadingContext = new StubLoadingContext();

	@Test
	public void withoutLoadingStrategy_withoutProjectionConstructor() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( ENTITY_NAME );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> b.addEntityType( IndexedEntity.class, ENTITY_NAME ) )
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
	public void withoutLoadingStrategy_withProjectionConstructor() {
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
				.withConfiguration( b -> b.addEntityType( IndexedEntity.class, ENTITY_NAME ) )
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
	public void withLoadingStrategy_withProjectionConstructor() {
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
		PersistenceTypeKey<IndexedEntity, Integer> typeKey =
				new PersistenceTypeKey<>( IndexedEntity.class, Integer.class );

		backendMock.expectAnySchema( ENTITY_NAME );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> b.addEntityType( IndexedEntity.class, ENTITY_NAME,
						c -> c.selectionLoadingStrategy( new StubSelectionLoadingStrategy<>( typeKey ) )
				) )
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
		backendMock.verifyExpectationsMet();

		IndexedEntity instance1 = new IndexedEntity( 1, "text1" );
		IndexedEntity instance2 = new IndexedEntity( 2, "text2" );
		IndexedEntity instance3 = new IndexedEntity( 3, "text3" );

		loadingContext.persistenceMap( typeKey ).put( instance1.id, instance1 );
		loadingContext.persistenceMap( typeKey ).put( instance2.id, instance2 );
		loadingContext.persistenceMap( typeKey ).put( instance3.id, instance3 );

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

			assertThat( session.search( IndexedEntity.class )
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

	@Test
	public void multiType() {
		class Model {
			@Indexed
			class IndexedEntityWithLoadingStrategy {
				public static final String NAME = "WithLoad";

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

			@Indexed
			class IndexedEntityWithLoadingStrategyChild extends IndexedEntityWithLoadingStrategy {
				public static final String NAME = "WithLoadChild";

				// A projection constructor unlike the parent class, but the loading strategy is inherited
				@ProjectionConstructor
				public IndexedEntityWithLoadingStrategyChild(@IdProjection Integer id, String text) {
					super( id, text );
				}
			}

			@Indexed
			class IndexedEntityWithProjectionConstructor {
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

			@Indexed
			class IndexedEntityWithProjectionConstructorChild extends IndexedEntityWithProjectionConstructor {
				public static final String NAME = "WithProjChild";

				// No projection constructor unlike the parent class, but there is a loading strategy
				public IndexedEntityWithProjectionConstructorChild(Integer id, String text) {
					super( id, text );
				}
			}
		}

		PersistenceTypeKey<Model.IndexedEntityWithLoadingStrategy, Integer> entityWithLoadingStrategyTypeKey =
				new PersistenceTypeKey<>( Model.IndexedEntityWithLoadingStrategy.class, Integer.class );
		PersistenceTypeKey<Model.IndexedEntityWithProjectionConstructorChild,
				Integer> entityWithProjectionConstructorChildTypeKey =
						new PersistenceTypeKey<>( Model.IndexedEntityWithProjectionConstructorChild.class, Integer.class );

		backendMock.expectAnySchema( Model.IndexedEntityWithLoadingStrategy.NAME );
		backendMock.expectAnySchema( Model.IndexedEntityWithLoadingStrategyChild.NAME );
		backendMock.expectAnySchema( Model.IndexedEntityWithProjectionConstructor.NAME );
		backendMock.expectAnySchema( Model.IndexedEntityWithProjectionConstructorChild.NAME );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> b
						.addEntityType(
								Model.IndexedEntityWithLoadingStrategy.class,
								Model.IndexedEntityWithLoadingStrategy.NAME,
								c -> c.selectionLoadingStrategy(
										new StubSelectionLoadingStrategy<>( entityWithLoadingStrategyTypeKey ) )
						)
						.addEntityType(
								Model.IndexedEntityWithLoadingStrategyChild.class,
								Model.IndexedEntityWithLoadingStrategyChild.NAME
						)
						.addEntityType(
								Model.IndexedEntityWithProjectionConstructor.class,
								Model.IndexedEntityWithProjectionConstructor.NAME
						)
						.addEntityType(
								Model.IndexedEntityWithProjectionConstructorChild.class,
								Model.IndexedEntityWithProjectionConstructorChild.NAME,
								c -> c.selectionLoadingStrategy( new StubSelectionLoadingStrategy<>(
										entityWithProjectionConstructorChildTypeKey ) )
						)
				)
				.withAnnotatedTypes(
						Model.IndexedEntityWithLoadingStrategy.class,
						Model.IndexedEntityWithLoadingStrategyChild.class,
						Model.IndexedEntityWithProjectionConstructor.class,
						Model.IndexedEntityWithProjectionConstructorChild.class
				)
				.setup();
		backendMock.verifyExpectationsMet();

		Model model = new Model();

		Model.IndexedEntityWithLoadingStrategy withLoadingStrategyInstance =
				model.new IndexedEntityWithLoadingStrategy( 1, "text1" );
		Model.IndexedEntityWithLoadingStrategyChild withLoadingStrategyChildInstance =
				model.new IndexedEntityWithLoadingStrategyChild( 2, "text2" );
		Model.IndexedEntityWithProjectionConstructor withProjectionConstructorInstance =
				model.new IndexedEntityWithProjectionConstructor( 3, "text3" );
		Model.IndexedEntityWithProjectionConstructorChild withProjectionConstructorChildInstance =
				model.new IndexedEntityWithProjectionConstructorChild( 3, "text3" );

		loadingContext.persistenceMap( entityWithLoadingStrategyTypeKey )
				.put( withLoadingStrategyInstance.id, withLoadingStrategyInstance );
		loadingContext.persistenceMap( entityWithLoadingStrategyTypeKey )
				.put( withLoadingStrategyChildInstance.id, withLoadingStrategyChildInstance );
		loadingContext.persistenceMap( entityWithProjectionConstructorChildTypeKey )
				.put( withProjectionConstructorChildInstance.id, withProjectionConstructorChildInstance );

		try ( SearchSession session = mapping.createSessionWithOptions()
				.loading( o -> o.context( StubLoadingContext.class, loadingContext ) )
				.build() ) {
			backendMock.expectSearchProjection(
					Arrays.asList( Model.IndexedEntityWithLoadingStrategy.NAME,
							Model.IndexedEntityWithLoadingStrategyChild.NAME,
							Model.IndexedEntityWithProjectionConstructor.NAME,
							Model.IndexedEntityWithProjectionConstructorChild.NAME ),
					StubSearchWorkBehavior.of(
							4,
							reference( Model.IndexedEntityWithLoadingStrategy.NAME,
									String.valueOf( withLoadingStrategyInstance.id ) ),
							// The loading strategy takes precedence over the projection constructor,
							// so expect IndexedEntityWithLoadingStrategyChild to be loaded.
							reference( Model.IndexedEntityWithLoadingStrategyChild.NAME,
									String.valueOf( withLoadingStrategyChildInstance.id ) ),
							new Pair<>( Model.IndexedEntityWithProjectionConstructor.NAME,
									Arrays.asList( String.valueOf( withProjectionConstructorInstance.id ),
											withProjectionConstructorInstance.text ) ),
							// The loading strategy takes precedence over the projection constructor,
							// so expect IndexedEntityWithProjectionConstructorChild to be loaded.
							reference( Model.IndexedEntityWithProjectionConstructorChild.NAME,
									String.valueOf( withProjectionConstructorChildInstance.id ) )
					)
			);

			assertThat( session.search( Arrays.asList( Model.IndexedEntityWithLoadingStrategy.class,
					Model.IndexedEntityWithProjectionConstructor.class
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
}
