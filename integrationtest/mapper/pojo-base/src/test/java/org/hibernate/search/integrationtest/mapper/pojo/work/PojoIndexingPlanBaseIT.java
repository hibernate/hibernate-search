/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Very basic tests for {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan}.
 * <p>
 * More advanced tests are implemented for the ORM mapper;
 * see {@code org.hibernate.search.integrationtest.mapper.orm.session.SearchIndexingPlanBaseIT}
 * in particular.
 */
public class PojoIndexingPlanBaseIT {

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper =
			JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "value", String.class )
				.objectField( "contained", b2 -> b2
						.field( "value", String.class ) )
		);

		mapping = setupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void simple() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity( 1 );
			IndexedEntity entity2 = new IndexedEntity( 2 );
			IndexedEntity entity3 = new IndexedEntity( 3 );

			session.indexingPlan().add( entity1 );
			session.indexingPlan().addOrUpdate( entity2 );
			session.indexingPlan().delete( entity3 );
			session.indexingPlan().delete( IndexedEntity.class, 4, null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( b -> b
							.identifier( "1" )
							.document( StubDocumentNode.document()
									.field( "value", entity1.value )
									.build()
							)
					)
					.addOrUpdate( b -> b
							.identifier( "2" )
							.document( StubDocumentNode.document()
									.field( "value", entity2.value )
									.build()
							)
					)
					.delete( b -> b.identifier( "3" ) )
					.delete( b -> b.identifier( "4" ) )
					.createdThenExecuted();
		}
	}

	/**
	 * Test the state inside indexing plans.
	 */
	@Test
	public void state() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity;

			BackendMock.DocumentWorkCallListContext expectations = backendMock.expectWorks( IndexedEntity.INDEX );

			// add then add
			entity = new IndexedEntity( 1 );
			session.indexingPlan().add( entity );
			session.indexingPlan().add( entity );
			expectations.add( "1", b -> b.field( "value", "val1" ) );

			// add then delete
			entity = new IndexedEntity( 2 );
			session.indexingPlan().add( entity );
			session.indexingPlan().delete( entity );
			// No work expected

			// add then update
			entity = new IndexedEntity( 3 );
			session.indexingPlan().add( entity );
			session.indexingPlan().addOrUpdate( entity );
			expectations.add( "3", b -> b.field( "value", "val3" ) );

			// add then update then delete
			entity = new IndexedEntity( 4 );
			session.indexingPlan().add( entity );
			session.indexingPlan().addOrUpdate( entity );
			session.indexingPlan().delete( entity );
			// No work expected

			// update then update
			entity = new IndexedEntity( 5 );
			session.indexingPlan().addOrUpdate( entity );
			session.indexingPlan().addOrUpdate( entity );
			expectations.addOrUpdate( "5", b -> b.field( "value", "val5" ) );

			// update then delete
			entity = new IndexedEntity( 6 );
			session.indexingPlan().addOrUpdate( entity );
			session.indexingPlan().delete( entity );
			expectations.delete( "6" );

			// update then delete then add
			entity = new IndexedEntity( 7 );
			session.indexingPlan().addOrUpdate( entity );
			session.indexingPlan().delete( entity );
			session.indexingPlan().add( entity );
			expectations.addOrUpdate( "7", b -> b.field( "value", "val7" ) );

			// delete then delete
			entity = new IndexedEntity( 8 );
			session.indexingPlan().delete( entity );
			session.indexingPlan().delete( entity );
			expectations.delete( "8" );

			// delete then add
			entity = new IndexedEntity( 9 );
			session.indexingPlan().delete( entity );
			session.indexingPlan().add( entity );
			expectations.addOrUpdate( "9", b -> b.field( "value", "val9" ) );

			// delete then add then update
			entity = new IndexedEntity( 10 );
			session.indexingPlan().delete( entity );
			session.indexingPlan().add( entity );
			session.indexingPlan().addOrUpdate( entity );
			expectations.addOrUpdate( "10", b -> b.field( "value", "val10" ) );

			expectations.createdThenExecuted();
		}
	}

	@Test
	public void dirtyPaths_root() {
		IndexedEntity indexed = new IndexedEntity( 1 );

		// Update with relevant dirty path
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( indexed, "value" );
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b.field( "value", "val1" ) )
					.createdThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Update with irrelevant dirty path
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( indexed, "notIndexed" );
		}
		backendMock.verifyExpectationsMet();

		// Update with a mix of relevant and irrelevant dirty paths
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( indexed, "value", "notIndexed" );
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b.field( "value", "val1" ) )
					.createdThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void dirtyPaths_contained() {
		IndexedEntity indexed = new IndexedEntity( 1 );
		ContainedEntity contained = new ContainedEntity( 2 );
		indexed.contained = contained;
		contained.containing = indexed;

		// Update with relevant dirty path
		try ( SearchSession session = mapping.createSession() ) {
			contained.value = "val3";
			session.indexingPlan().addOrUpdate( 2, null, contained, "value" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "value", "val1" )
							.objectField( "contained", b2 -> b2
									.field( "value", "val3" ) ) )
					.createdThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Update with irrelevant dirty path
		try ( SearchSession session = mapping.createSession() ) {
			contained.notIndexed = "foo";
			session.indexingPlan().addOrUpdate( 2, null, contained, "notIndexed" );
		}
		backendMock.verifyExpectationsMet();

		// Update with a mix of relevant and irrelevant dirty paths
		try ( SearchSession session = mapping.createSession() ) {
			contained.value = "val4";
			contained.notIndexed = "bar";
			session.indexingPlan().addOrUpdate( 2, null, contained, "value", "notIndexed" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "value", "val1" )
							.objectField( "contained", b2 -> b2
									.field( "value", "val4" ) ) )
					.createdThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test when the entity is null and must be loaded.
	 */
	@Test
	public void nullEntity() {
		@SuppressWarnings("unchecked")
		SelectionEntityLoader<IndexedEntity> loaderMock = mock( SelectionEntityLoader.class );

		try ( SearchSession session = mapping.createSessionWithOptions()
				.loading( o -> o.selectionLoadingStrategy( IndexedEntity.class, (includedTypes, options) -> loaderMock ) )
				.build() ) {
			IndexedEntity entity1 = new IndexedEntity( 1 );
			IndexedEntity entity2 = new IndexedEntity( 2 );

			session.indexingPlan().add( IndexedEntity.class, 1, null );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, 2, null );
			session.indexingPlan().delete( IndexedEntity.class, 3, null );
			session.indexingPlan().delete( IndexedEntity.class, 4, null );

			when( loaderMock.load( Arrays.asList( 1, 2 ), null ) )
					.thenReturn( Arrays.asList( entity1, entity2 ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( b -> b
							.identifier( "1" )
							.document( StubDocumentNode.document()
									.field( "value", entity1.value )
									.build()
							)
					)
					.addOrUpdate( b -> b
							.identifier( "2" )
							.document( StubDocumentNode.document()
									.field( "value", entity2.value )
									.build()
							)
					)
					.delete( b -> b.identifier( "3" ) )
					.delete( b -> b.identifier( "4" ) )
					.createdThenExecuted();
		}
	}

	/**
	 * Test the state inside indexing plans when the entity is null and must be loaded.
	 */
	@Test
	public void nullEntity_state() {
		@SuppressWarnings("unchecked")
		SelectionEntityLoader<IndexedEntity> loaderMock = mock( SelectionEntityLoader.class );
		List<Integer> idsToLoad = new ArrayList<>();
		List<IndexedEntity> loadedEntities = new ArrayList<>();

		try ( SearchSession session = mapping.createSessionWithOptions()
				.loading( o -> o.selectionLoadingStrategy( IndexedEntity.class, (includedTypes, options) -> loaderMock ) )
				.build() ) {
			IndexedEntity entity;

			BackendMock.DocumentWorkCallListContext expectations = backendMock.expectWorks( IndexedEntity.INDEX );

			// add then add
			entity = new IndexedEntity( 1 );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			idsToLoad.add( entity.id );
			loadedEntities.add( entity );
			expectations.add( "1", b -> b.field( "value", "val1" ) );

			// add then delete
			entity = new IndexedEntity( 2 );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			// No work expected

			// add then update
			entity = new IndexedEntity( 3 );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, entity.id, null );
			idsToLoad.add( entity.id );
			loadedEntities.add( entity );
			expectations.add( "3", b -> b.field( "value", "val3" ) );

			// add then update then delete
			entity = new IndexedEntity( 4 );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, entity.id, null );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			// No work expected

			// update then update
			entity = new IndexedEntity( 5 );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, entity.id, null );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, entity.id, null );
			idsToLoad.add( entity.id );
			loadedEntities.add( entity );
			expectations.addOrUpdate( "5", b -> b.field( "value", "val5" ) );

			// update then delete
			entity = new IndexedEntity( 6 );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, entity.id, null );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			expectations.delete( "6" );

			// update then delete then add
			entity = new IndexedEntity( 7 );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, entity.id, null );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			idsToLoad.add( entity.id );
			loadedEntities.add( entity );
			expectations.addOrUpdate( "7", b -> b.field( "value", "val7" ) );

			// delete then delete
			entity = new IndexedEntity( 8 );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			expectations.delete( "8" );

			// delete then add
			entity = new IndexedEntity( 9 );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			idsToLoad.add( entity.id );
			loadedEntities.add( entity );
			expectations.addOrUpdate( "9", b -> b.field( "value", "val9" ) );

			// delete then add then update
			entity = new IndexedEntity( 10 );
			session.indexingPlan().delete( IndexedEntity.class, entity.id, null );
			session.indexingPlan().add( IndexedEntity.class, entity.id, null );
			session.indexingPlan().addOrUpdate( IndexedEntity.class, entity.id, null );
			idsToLoad.add( entity.id );
			loadedEntities.add( entity );
			expectations.addOrUpdate( "10", b -> b.field( "value", "val10" ) );

			when( loaderMock.load( idsToLoad, null ) ).thenReturn( loadedEntities );

			expectations.createdThenExecuted();
		}

		verify( loaderMock ).load( any(), any() );
	}

	@Test
	public void failure() {
		RuntimeException simulatedFailure = new RuntimeException( "Indexing failure" );
		assertThatThrownBy( () -> {
			try ( SearchSession session = mapping.createSession() ) {
				CompletableFuture<?> failingFuture = new CompletableFuture<>();
				failingFuture.completeExceptionally( simulatedFailure );

				IndexedEntity entity1 = new IndexedEntity( 1 );

				session.indexingPlan().add( entity1 );

				backendMock.expectWorks( IndexedEntity.INDEX )
						.add( b -> b
								.identifier( "1" )
								.document( StubDocumentNode.document()
										.field( "value", entity1.value )
										.build()
								)
						)
						.createdThenExecuted( failingFuture );
			}
		} )
				.isSameAs( simulatedFailure );
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@DocumentId
		private Integer id;

		@GenericField
		private String value;

		private String notIndexed;

		@IndexedEmbedded
		private ContainedEntity contained;

		public IndexedEntity(int id) {
			this.id = id;
			this.value = "val" + id;
		}

	}

	public static final class ContainedEntity {

		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "contained")))
		private IndexedEntity containing;

		@GenericField
		private String value;

		private String notIndexed;

		public ContainedEntity(int id) {
			this.value = "val" + id;
		}

	}
}
