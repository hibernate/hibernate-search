/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("unused")
public class IndexedBaseIT {

	@Rule
	public BackendMock defaultBackendMock = new BackendMock();

	@Rule
	public BackendMock backend2Mock = new BackendMock();

	@Rule
	public BackendMock backend3Mock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), defaultBackendMock );

	@Rule
	public StandalonePojoMappingSetupHelper multiBackendSetupHelper;

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	public IndexedBaseIT() {
		Map<String, BackendMock> namedBackendMocks = new LinkedHashMap<>();
		namedBackendMocks.put( "backend2", backend2Mock );
		namedBackendMocks.put( "backend3", backend3Mock );
		multiBackendSetupHelper = StandalonePojoMappingSetupHelper.withBackendMocks(
				MethodHandles.lookup(), defaultBackendMock, namedBackendMocks
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3705")
	public void implicitIndexName_defaultEntityName() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}

		defaultBackendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> b
				.field( "text", String.class )
		);
		setupHelper.start()
				.setup( IndexedEntity.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3705")
	public void implicitIndexName_explicitEntityName() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}

		defaultBackendMock.expectSchema( "myEntityName", b -> b
				.field( "text", String.class )
		);
		setupHelper.start()
				.withConfiguration( b -> {
					b.annotationMapping().add( IndexedEntity.class );
					b.addEntityType( IndexedEntity.class, "myEntityName" );
				} )
				.setup();
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void explicitIndexName() {
		@Indexed(index = "explicitIndexName")
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}

		defaultBackendMock.expectSchema( "explicitIndexName", b -> b
				.field( "text", String.class )
		);
		setupHelper.start()
				.setup( IndexedEntity.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void nonDefaultBackend() {
		@Indexed(index = "index", backend = "backend2")
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}

		backend2Mock.expectSchema( "index", b -> b
				.field( "text", String.class )
		);
		multiBackendSetupHelper.start()
				.setup( IndexedEntity.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void multiBackend() {
		@Indexed(index = "index1", backend = "backend2")
		class IndexedEntity1 {
			@DocumentId
			Integer id;
			@GenericField
			String text1;
		}
		@Indexed(index = "index2", backend = "backend3")
		class IndexedEntity2 {
			@DocumentId
			Integer id;
			@GenericField
			String text2;
		}

		backend2Mock.expectSchema( "index1", b -> b
				.field( "text1", String.class )
		);
		backend3Mock.expectSchema( "index2", b -> b
				.field( "text2", String.class )
		);
		multiBackendSetupHelper.start()
				.setup( IndexedEntity1.class, IndexedEntity2.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	public void inheritance() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			public Integer getId() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		class IndexedEntitySubClass extends IndexedEntity {
			@GenericField
			public Integer getNumber() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		defaultBackendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> b
				.field( "text", String.class )
		);
		// The subclass should be indexed too.
		defaultBackendMock.expectSchema( IndexedEntitySubClass.class.getSimpleName(), b -> b
				.field( "text", String.class )
				.field( "number", Integer.class )
		);
		setupHelper.start().setup( IndexedEntity.class, IndexedEntitySubClass.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	public void inheritance_abstract() {
		@Indexed
		abstract class AbstractIndexedEntity {
			@DocumentId
			public Integer getId() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		class IndexedEntitySubClass1 extends AbstractIndexedEntity {
			@GenericField
			public Long getNumber1() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		class IndexedEntitySubClass2 extends AbstractIndexedEntity {
			@GenericField
			public Integer getNumber2() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		// The abstract class should not be indexed.
		// The concrete subclasses should be indexed.
		defaultBackendMock.expectSchema( IndexedEntitySubClass1.class.getSimpleName(), b -> b
				.field( "text", String.class )
				.field( "number1", Long.class )
		);
		defaultBackendMock.expectSchema( IndexedEntitySubClass2.class.getSimpleName(), b -> b
				.field( "text", String.class )
				.field( "number2", Integer.class )
		);
		setupHelper.start().setup( AbstractIndexedEntity.class,
				IndexedEntitySubClass1.class, IndexedEntitySubClass2.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	public void inheritance_abstract_subclass() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			public Integer getId() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		abstract class AbstractIndexedEntitySubClass extends IndexedEntity {
			@GenericField
			public Integer getNumber() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		class IndexedEntitySubClass1 extends AbstractIndexedEntitySubClass {
			@GenericField
			public Long getNumber1() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		class IndexedEntitySubClass2 extends AbstractIndexedEntitySubClass {
			@GenericField
			public Integer getNumber2() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		defaultBackendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> b
				.field( "text", String.class )
		);
		// The abstract subclass should not be indexed.
		// The concrete subclasses should be indexed.
		defaultBackendMock.expectSchema( IndexedEntitySubClass1.class.getSimpleName(), b -> b
				.field( "text", String.class )
				.field( "number", Integer.class )
				.field( "number1", Long.class )
		);
		defaultBackendMock.expectSchema( IndexedEntitySubClass2.class.getSimpleName(), b -> b
				.field( "text", String.class )
				.field( "number", Integer.class )
				.field( "number2", Integer.class )
		);
		setupHelper.start().setup( IndexedEntity.class, AbstractIndexedEntitySubClass.class,
				IndexedEntitySubClass1.class, IndexedEntitySubClass2.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	public void inheritance_disabled() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			public Integer getId() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		@Indexed(enabled = false)
		class IndexedEntitySubClass extends IndexedEntity {
			@GenericField
			public Integer getNumber() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		defaultBackendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> b
				.field( "text", String.class )
		);
		// The subclass should NOT be indexed.
		setupHelper.start().setup( IndexedEntity.class, IndexedEntitySubClass.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	public void inheritance_explicitAttributes() {
		@Indexed(backend = "backend2", index = "parentClassIndex")
		class IndexedEntity {
			@DocumentId
			public Integer getId() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		class IndexedEntitySubClass extends IndexedEntity {
			@GenericField
			public Integer getNumber() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		backend2Mock.expectSchema( "parentClassIndex", b -> b
				.field( "text", String.class )
		);
		// The subclass should be indexed and use the same backend,
		// but the default index name for that entity (not the parent index name).
		backend2Mock.expectSchema( IndexedEntitySubClass.class.getSimpleName(), b -> b
				.field( "text", String.class )
				.field( "number", Integer.class )
		);
		multiBackendSetupHelper.start().setup( IndexedEntity.class, IndexedEntitySubClass.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	public void inheritance_override_backend() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			public Integer getId() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		@Indexed(backend = "backend2")
		class IndexedEntitySubClass extends IndexedEntity {
			@GenericField
			public Integer getNumber() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		defaultBackendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> b
				.field( "text", String.class )
		);
		// The subclass should be indexed too, but in a different backend.
		backend2Mock.expectSchema( IndexedEntitySubClass.class.getSimpleName(), b -> b
				.field( "text", String.class )
				.field( "number", Integer.class )
		);
		multiBackendSetupHelper.start().setup( IndexedEntity.class, IndexedEntitySubClass.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1231")
	public void inheritance_override_index() {
		@Indexed(index = "parentClassIndex")
		class IndexedEntity {
			@DocumentId
			public Integer getId() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
			@GenericField
			public String getText() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		@Indexed(index = "subClassIndex")
		class IndexedEntitySubClass extends IndexedEntity {
			@GenericField
			public Integer getNumber() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		defaultBackendMock.expectSchema( "parentClassIndex", b -> b
				.field( "text", String.class )
		);
		// The subclass should be indexed too, with the given explicit name.
		defaultBackendMock.expectSchema( "subClassIndex", b -> b
				.field( "text", String.class )
				.field( "number", Integer.class )
		);
		setupHelper.start().setup( IndexedEntity.class, IndexedEntitySubClass.class );
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	public void error_indexedWithoutEntityMetadata() {
		@Indexed
		class IndexedWithoutEntityMetadata {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}
		assertThatThrownBy(
				() -> setupHelper.start()
						// Do not mention the type is an entity type here, on purpose, to trigger the failure
						.withAnnotatedTypes( IndexedWithoutEntityMetadata.class )
						.setup()
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedWithoutEntityMetadata.class.getName() )
						.failure( "Unable to index type '" + IndexedWithoutEntityMetadata.class.getName()
								+ "': this type is not an entity type."
								+ " If you only expect subtypes to be instantiated, make this type abstract."
								+ " If you expect this exact type to be instantiated and want it to be indexed, make it an entity type."
								+ " Otherwise, ensure this type and its subtypes are never indexed by removing the @Indexed annotation"
								+ " or by annotating the type with @Indexed(enabled = false)." ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void routingBinder() {
		@Indexed(routingBinder = @RoutingBinderRef(type = StaticCounterRoutingBinder.class))
		class IndexedEntity {
			@DocumentId
			Integer id;
		}

		defaultBackendMock.expectSchema( IndexedEntity.class.getSimpleName(), b -> { } );

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		defaultBackendMock.verifyExpectationsMet();

		assertThat( staticCounters.get( StaticCounterRoutingBinder.KEY ) ).isEqualTo( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void routingBinder_failure() {
		@Indexed(routingBinder = @RoutingBinderRef(type = FailingRoutingBinder.class))
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "Simulated failure" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4006")
	public void moreTypesTargetSameIndex() {
		@Indexed(index = "indexName")
		class IndexedEntityA {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}
		@Indexed(index = "indexName")
		class IndexedEntityB {
			@DocumentId
			Integer id;
			@GenericField
			String text;
		}

		assertThatThrownBy( () -> setupHelper.start().setup( IndexedEntityA.class, IndexedEntityB.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple entity types mapped to index 'indexName'",
						"IndexedEntityA", "IndexedEntityB",
						"Each indexed type must be mapped to its own, dedicated index" );
	}

	public static class StaticCounterRoutingBinder implements RoutingBinder {
		private static final StaticCounters.Key KEY = StaticCounters.createKey();

		@Override
		public void bind(RoutingBindingContext context) {
			assertThat( context ).isNotNull();
			assertThat( context.beanResolver() ).isNotNull();
			assertThat( context.bridgedElement() ).isNotNull();
			assertThat( context.dependencies() ).isNotNull();
			context.dependencies().useRootOnly();
			context.bridge( Object.class, new RoutingBridge<Object>() {
				@Override
				public void route(DocumentRoutes routes, Object entityIdentifier, Object indexedEntity,
						RoutingBridgeRouteContext context) {
					throw new AssertionFailure( "This method should not be called." );
				}

				@Override
				public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, Object indexedEntity,
						RoutingBridgeRouteContext context) {
					throw new AssertionFailure( "This method should not be called." );
				}
			} );
			StaticCounters.get().increment( KEY );
		}
	}

	public static class FailingRoutingBinder implements RoutingBinder {
		@Override
		public void bind(RoutingBindingContext context) {
			throw new RuntimeException( "Simulated failure" );
		}
	}
}
