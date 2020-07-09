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
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setValue( "val1" );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setValue( "val2" );
			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setValue( "val3" );

			session.indexingPlan().add( entity1 );
			session.indexingPlan().addOrUpdate( entity2 );
			session.indexingPlan().delete( entity3 );
			session.indexingPlan().purge( IndexedEntity.class, 4, null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( b -> b
							.identifier( "1" )
							.document( StubDocumentNode.document()
									.field( "value", entity1.getValue() )
									.build()
							)
					)
					.update( b -> b
							.identifier( "2" )
							.document( StubDocumentNode.document()
									.field( "value", entity2.getValue() )
									.build()
							)
					)
					.delete( b -> b.identifier( "3" ) )
					.delete( b -> b.identifier( "4" ) )
					.processedThenExecuted();
		}
	}

	@Test
	public void failure() {
		RuntimeException simulatedFailure = new RuntimeException( "Indexing failure" );
		assertThatThrownBy( () -> {
			try ( SearchSession session = mapping.createSession() ) {
				CompletableFuture<?> failingFuture = new CompletableFuture<>();
				failingFuture.completeExceptionally( simulatedFailure );

				IndexedEntity entity1 = new IndexedEntity();
				entity1.setId( 1 );
				entity1.setValue( "val1" );

				session.indexingPlan().add( entity1 );

				backendMock.expectWorks( IndexedEntity.INDEX )
						.add( b -> b
								.identifier( "1" )
								.document( StubDocumentNode.document()
										.field( "value", entity1.getValue() )
										.build()
								)
						)
						.processedThenExecuted( failingFuture );
			}
		} )
				.isSameAs( simulatedFailure );
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private String value;

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}
}
