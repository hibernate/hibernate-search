/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "value", String.class )
		);

		mapping = setupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity( 1 );
			IndexedEntity entity2 = new IndexedEntity( 2 );
			IndexedEntity entity3 = new IndexedEntity( 3 );

			session.indexingPlan().add( entity1 );
			session.indexingPlan().addOrUpdate( entity2 );
			session.indexingPlan().delete( entity3 );
			session.indexingPlan().purge( IndexedEntity.class, 4, null );

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
					.delete( b -> b.identifier( "4" ) );
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
		}
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
						.createAndExecuteFollowingWorks( failingFuture )
						.add( b -> b
								.identifier( "1" )
								.document( StubDocumentNode.document()
										.field( "value", entity1.value )
										.build()
								)
						);
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

		public IndexedEntity(int id) {
			this.id = id;
			this.value = "val" + id;
		}

	}
}
