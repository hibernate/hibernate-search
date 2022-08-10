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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

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
							"Cannot project on entity type '" + ENTITY_NAME + "': this type cannot be loaded from an external datasource,"
									+ " and the documents from the index cannot be projected to its Java class '" + IndexedEntity.class.getName() + "'",
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
			// Necessary because there's no way to project on the id with @ProjectionConstructor yet
			// TODO HSEARCH-4574 remove this field and use @IdProjection or similar in the constructor instead
			@GenericField
			public Integer id;
			@FullTextField
			public String text;

			@ProjectionConstructor
			public IndexedEntity(Integer id, String text) {
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
							Arrays.asList( instance1.id, instance1.text ),
							Arrays.asList( instance2.id, instance2.text ),
							Arrays.asList( instance3.id, instance3.text )
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
			// Necessary because there's no way to project on the id with @ProjectionConstructor yet
			// TODO HSEARCH-4574 remove this field and use @IdProjection or similar in the constructor instead
			@GenericField
			public Integer id;
			@FullTextField
			public String text;

			@ProjectionConstructor
			public IndexedEntity(Integer id, String text) {
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

}
