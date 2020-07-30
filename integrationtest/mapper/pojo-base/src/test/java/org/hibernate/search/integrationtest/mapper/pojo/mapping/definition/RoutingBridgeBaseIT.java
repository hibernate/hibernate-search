/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) routing bridges.
 * <p>
 * Does not test the effect of routing in depth for all operations; this is tested in
 * {@link org.hibernate.search.integrationtest.mapper.pojo.work.AbstractPojoIndexingOperationIT}.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-3108")
public class RoutingBridgeBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void invalidTypeForRoutingBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						context.bridge( Integer.class, new UnusedRoutingBridge<>() );
					} );
				} )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "Routing bridge '" + UnusedRoutingBridge.TOSTRING
								+ "' cannot be applied to entity type '" + IndexedEntity.class.getName() + "'" )
						.build()
				);
	}

	@Test
	public void conflictingRoutingBridgeAndRoutingKeyBinder() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			String stringProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public String getStringProperty() {
				return stringProperty;
			}
		}
		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						context.bridge( IndexedEntity.class, new UnusedRoutingBridge<>() );
					} );
					typeMapping.routingKeyBinder( new UnusedRoutingKeyBinder() );
				} )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "Routing bridge '" + UnusedRoutingBridge.TOSTRING
								+ "' is already assigned to this entity; cannot apply routing key binder '"
								+ UnusedRoutingKeyBinder.TOSTRING + "'" )
						.build()
				);
	}

	@Test
	public void missingRoute() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}

		class NoRouteRoutingBridge implements RoutingBridge<IndexedEntity> {
			@Override
			public String toString() {
				return "NoRouteRoutingBridge";
			}

			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				// Do nothing
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> { } );

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed()
							.routingBinder( context -> {
								context.bridge( IndexedEntity.class, new NoRouteRoutingBridge() );
							} );
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			assertThatThrownBy( session::close )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Routing bridge 'NoRouteRoutingBridge' did not define any route",
							"Exactly one route must be defined",
							"or you can call notIndexed() to explicitly indicate no route is necessary" );
		}
		backendMock.verifyExpectationsMet();
	}


	@Test
	public void multipleRoutes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}

		class TwoRoutesRoutingBridge implements RoutingBridge<IndexedEntity> {
			@Override
			public String toString() {
				return "TwoRoutesRoutingBridge";
			}

			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				routes.addRoute().routingKey( "foo" );
				routes.addRoute();
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> { } );

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed()
							.routingBinder( context -> {
								context.bridge( IndexedEntity.class, new TwoRoutesRoutingBridge() );
							} );
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			assertThatThrownBy( session::close )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Routing bridge 'TwoRoutesRoutingBridge' defined multiple routes",
							"At most one route must be defined" );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void accessors() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			String stringProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public String getStringProperty() {
				return stringProperty;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> { } );

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed()
							.routingBinder( context -> {
								PojoElementAccessor<String> pojoPropertyAccessor =
										context.bridgedElement().property( "stringProperty" )
												.createAccessor( String.class );
								context.bridge(
										IndexedEntity.class,
										(DocumentRoutes routes, Object entityId, IndexedEntity entity,
												RoutingBridgeRouteContext context1) -> {
											routes.addRoute().routingKey( pojoPropertyAccessor.read( entity ) );
										}
								);
							} );
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		entity.stringProperty = "some string";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( b -> b.identifier( "1" ).routingKey( entity.stringProperty )
							.document( StubDocumentNode.document().build() ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty = "some string 2";
			session.indexingPlan().addOrUpdate( entity );

			backendMock.expectWorks( INDEX_NAME )
					.update( b -> b.identifier( "1" ).routingKey( entity.stringProperty )
							.document( StubDocumentNode.document().build() ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().delete( entity );

			backendMock.expectWorks( INDEX_NAME )
					.delete( b -> b.identifier( "1" ).routingKey( entity.stringProperty ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void accessors_incompatibleRequestedType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			String stringProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public String getStringProperty() {
				return stringProperty;
			}
		}
		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						context.bridgedElement().property( "stringProperty" )
								.createAccessor( Integer.class );
						context.bridge( IndexedEntity.class, new UnusedRoutingBridge<>() );
					} );
				} )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Requested incompatible type for '.stringProperty<no value extractors>'",
								"'" + Integer.class.getName() + "'"
						)
						.build()
				);
	}

	private static class UnusedRoutingBridge<T> implements RoutingBridge<T> {
		public static final String TOSTRING = "UnusedRoutingBridge";

		@Override
		public String toString() {
			return TOSTRING;
		}

		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, Object indexedEntity,
				RoutingBridgeRouteContext context) {
			throw new AssertionFailure( "Should not be called" );
		}
	}

	private static class UnusedRoutingKeyBinder implements RoutingKeyBinder {
		public static final String TOSTRING = "UnusedRoutingKeyBinder";

		@Override
		public String toString() {
			return TOSTRING;
		}

		@Override
		public void bind(RoutingKeyBindingContext context) {
			throw new AssertionFailure( "Should not be called" );
		}
	}
}
