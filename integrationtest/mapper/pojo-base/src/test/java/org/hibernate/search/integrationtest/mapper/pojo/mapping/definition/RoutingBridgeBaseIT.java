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
			@DocumentId
			Integer id;
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
						.failure( "Invalid routing bridge for entity type '" + IndexedEntity.class.getName()
										+ "': '" + UnusedRoutingBridge.TOSTRING + "'",
								"This bridge expects an entity type extending '" + Integer.class.getName() )
						.build()
				);
	}

	@Test
	public void currentRoute_missing() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}

		class NoCurrentRouteRoutingBridge implements RoutingBridge<IndexedEntity> {
			@Override
			public String toString() {
				return "NoCurrentRouteRoutingBridge";
			}

			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				// Do nothing
			}

			@Override
			public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				routes.addRoute();
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> { } );

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed()
							.routingBinder( context -> {
								context.dependencies().useRootOnly(); // Irrelevant
								context.bridge( IndexedEntity.class, new NoCurrentRouteRoutingBridge() );
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
					.hasMessageContainingAll(
							"Incorrect routing bridge implementation:"
									+ " routing bridge 'NoCurrentRouteRoutingBridge' did not define any current route.",
							"In the implementation of RoutingBridge.route(...)",
							"define exactly one current route by calling 'routes.addRoute()',"
									+ " or explicitly indicate indexing is not required by calling 'routes.notIndexed()'."
					);
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void previousRoutes_missing() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}

		class NoPreviousRouteRoutingBridge implements RoutingBridge<IndexedEntity> {
			@Override
			public String toString() {
				return "NoPreviousRouteRoutingBridge";
			}

			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				routes.addRoute();
			}

			@Override
			public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
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
								context.dependencies().useRootOnly(); // Irrelevant
								context.bridge( IndexedEntity.class, new NoPreviousRouteRoutingBridge() );
							} );
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( entity );

			assertThatThrownBy( session::close )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Incorrect routing bridge implementation:"
									+ " routing bridge 'NoPreviousRouteRoutingBridge' did not define any previous route.",
							"In the implementation of RoutingBridge.previousRoutes(...)",
							"define at least one previous route by calling 'routes.addRoute()' at least once,"
									+ " or explicitly indicate no prior indexing was performed by calling 'routes.notIndexed()'"
					);
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().delete( entity );

			assertThatThrownBy( session::close )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Incorrect routing bridge implementation:"
									+ " routing bridge 'NoPreviousRouteRoutingBridge' did not define any previous route.",
							"In the implementation of RoutingBridge.previousRoutes(...)",
							"define at least one previous route by calling 'routes.addRoute()' at least once,"
									+ " or explicitly indicate no prior indexing was performed by calling 'routes.notIndexed()'"
					);
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void currentRoute_multiple() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}

		class TwoCurrentRoutesRoutingBridge implements RoutingBridge<IndexedEntity> {
			@Override
			public String toString() {
				return "TwoCurrentRoutesRoutingBridge";
			}

			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				routes.addRoute().routingKey( "foo" );
				routes.addRoute();
			}

			@Override
			public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				routes.addRoute();
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> { } );

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed()
							.routingBinder( context -> {
								context.dependencies().useRootOnly(); // Irrelevant
								context.bridge( IndexedEntity.class, new TwoCurrentRoutesRoutingBridge() );
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
					.hasMessageContainingAll(
							"Incorrect routing bridge implementation:"
									+ " routing bridge 'TwoCurrentRoutesRoutingBridge' defined multiple current routes.",
							"In the implementation of RoutingBridge.route(...)",
							"define at most one current route by calling 'routes.addRoute()' at most once."
					);
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void accessors() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
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
								context.bridge( IndexedEntity.class,
										new RoutingBridge<IndexedEntity>() {
											@Override
											public void route(DocumentRoutes routes, Object entityIdentifier,
													IndexedEntity indexedEntity, RoutingBridgeRouteContext context) {
												routes.addRoute().routingKey( pojoPropertyAccessor.read( indexedEntity ) );
											}

											@Override
											public void previousRoutes(DocumentRoutes routes, Object entityIdentifier,
													IndexedEntity indexedEntity, RoutingBridgeRouteContext context) {
												// Assume "stringProperty" can only take values from a finite set
												routes.addRoute().routingKey( "some string" );
												routes.addRoute().routingKey( "some string 2" );
												routes.addRoute().routingKey( "some string 3" );
											}
										} );
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
			// A change to "stringProperty" should be considered as a change requiring reindexing,
			// since it's used in routing.
			session.indexingPlan().addOrUpdate( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "some string" ) )
					.delete( b -> b.identifier( "1" ).routingKey( "some string 3" ) )
					.update( b -> b.identifier( "1" ).routingKey( entity.stringProperty )
							.document( StubDocumentNode.document().build() ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().delete( entity );

			backendMock.expectWorks( INDEX_NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "some string" ) )
					.delete( b -> b.identifier( "1" ).routingKey( "some string 3" ) )
					.delete( b -> b.identifier( "1" ).routingKey( entity.stringProperty ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void accessors_incompatibleRequestedType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
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
						.failure( "'.stringProperty<no value extractors>' cannot be assigned to '"
								+ Integer.class.getName() + "'" )
						.build()
				);
	}

	/**
	 * Basic test checking that a "normal" custom type bridge will work as expected
	 * when relying on explicit dependency declaration.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	public void explicitDependencies() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		backendMock.expectSchema( INDEX_NAME, b -> { } );

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						context.dependencies().use( "stringProperty" );
						context.bridge(
								IndexedEntity.class,
								new RoutingBridge<IndexedEntity>() {
									@Override
									public void route(DocumentRoutes routes, Object entityIdentifier,
											IndexedEntity indexedEntity, RoutingBridgeRouteContext context) {
										routes.addRoute().routingKey( indexedEntity.stringProperty );
									}

									@Override
									public void previousRoutes(DocumentRoutes routes, Object entityIdentifier,
											IndexedEntity indexedEntity, RoutingBridgeRouteContext context) {
										// Assume "stringProperty" can only take values from a finite set
										routes.addRoute().routingKey( "some string" );
										routes.addRoute().routingKey( "some string 2" );
										routes.addRoute().routingKey( "some string 3" );
									}
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
			// A change to "stringProperty" should be considered as a change requiring reindexing,
			// since it's used in routing.
			session.indexingPlan().addOrUpdate( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "some string" ) )
					.delete( b -> b.identifier( "1" ).routingKey( "some string 3" ) )
					.update( b -> b.identifier( "1" ).routingKey( entity.stringProperty )
							.document( StubDocumentNode.document().build() ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().delete( entity );

			backendMock.expectWorks( INDEX_NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "some string" ) )
					.delete( b -> b.identifier( "1" ).routingKey( "some string 3" ) )
					.delete( b -> b.identifier( "1" ).routingKey( entity.stringProperty ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void explicitDependencies_error_invalidProperty() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						context.dependencies().use( "doesNotExist" );
						context.bridge( IndexedEntity.class, new UnusedRoutingBridge<>() );
					} );
				} )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "No readable property named 'doesNotExist' on type '"
								+ IndexedEntity.class.getName() + "'" )
						.build() );
	}

	@Test
	public void missingDependencyDeclaration() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						// Do not declare any dependency
						context.bridge( IndexedEntity.class, new UnusedRoutingBridge<>() );
					} );
				} )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "Incorrect binder implementation",
								"the binder did not declare any dependency to the entity model during binding.",
								" Declare dependencies using context.dependencies().use(...) or,"
										+ " if the bridge really does not depend on the entity model, context.dependencies().useRootOnly()" )
						.build() );
	}

	@Test
	public void inconsistentDependencyDeclaration() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						// Declare no dependency, but also a dependency: this is inconsistent.
						context.dependencies()
								.use( "stringProperty" )
								.useRootOnly();
						context.bridge( IndexedEntity.class, new UnusedRoutingBridge<>() );
					} );
				} )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "Incorrect binder implementation",
								"the binder called context.dependencies().useRootOnly() during binding,"
										+ " but also declared extra dependencies to the entity model." )
						.build() );
	}

	@Test
	public void useRootOnly() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		backendMock.expectSchema( INDEX_NAME, b -> { } );

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					TypeMappingStep typeMapping = b.programmaticMapping().type( IndexedEntity.class );
					typeMapping.indexed().routingBinder( context -> {
						context.dependencies().useRootOnly();
						context.bridge( IndexedEntity.class,
								new RoutingBridge<IndexedEntity>() {
									@Override
									public void route(DocumentRoutes routes, Object entityIdentifier,
											IndexedEntity indexedEntity, RoutingBridgeRouteContext context) {
										routes.addRoute().routingKey( "route/" + entityIdentifier );
									}

									@Override
									public void previousRoutes(DocumentRoutes routes, Object entityIdentifier,
											IndexedEntity indexedEntity, RoutingBridgeRouteContext context) {
										// The route never changes
										route( routes, entityIdentifier, indexedEntity, context );
									}
								} );
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
					.add( b -> b.identifier( "1" ).routingKey( "route/1" )
							.document( StubDocumentNode.document().build() ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty = "some string 2";
			// A change to "stringProperty" should be ignored,
			// since it's not used for routing or indexing.
			session.indexingPlan().addOrUpdate( entity, new String[] { "stringProperty" } );
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().addOrUpdate( entity );

			backendMock.expectWorks( INDEX_NAME )
					.update( b -> b.identifier( "1" ).routingKey( "route/1" )
							.document( StubDocumentNode.document().build() ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().delete( entity );

			backendMock.expectWorks( INDEX_NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "route/1" ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
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

		@Override
		public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, T indexedEntity,
				RoutingBridgeRouteContext context) {
			throw new AssertionFailure( "Should not be called" );
		}
	}
}
