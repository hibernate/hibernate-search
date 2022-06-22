/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.lifecycle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Consumer;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubContainerExtractor;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubBridge;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManagerBuilder;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test that all resources are closed as expected upon shutdown, or when startup fails.
 */
public class CleanupIT {
	private static final StartupStubBridge.CounterKeys IDENTIFIER_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys ROUTING_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys TYPE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys PROPERTY_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubContainerExtractor.CounterKeys CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS =
			StartupStubContainerExtractor.createKeys();
	private static final StartupStubBridge.CounterKeys VALUE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private CloseableSearchMapping mapping;

	@Test
	public void successfulBuilding() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );
		backendMock.expectAnySchema( OtherIndexedEntity.INDEX );

		startup( mappingDefinition -> {
			TypeMappingStep otherIndexedEntityMapping = mappingDefinition.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.INDEX )
					.routingBinder( StartupStubBridge.binder( ROUTING_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.binder( StartupStubBridge.binder( TYPE_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "id" )
					.documentId()
							.identifierBinder( StartupStubBridge.binder( Integer.class, IDENTIFIER_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "text" )
					.binder( StartupStubBridge.binder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
					.genericField( "otherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractor( StartupStubContainerExtractor.NAME );
		} );

		backendMock.verifyExpectationsMet();

		// Exactly 2 index managers are expected
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertEquals( 2, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( StubIndexManager.STOP_COUNTER_KEY ) );

		assertEquals( 2, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertEquals( 2, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) );

		// Extra type and property bridges should have been created...
		assertEquals( 2 + 1, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertEquals( 2 + 1, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		// ... but closed immediately because they were only contributing fields that were filtered out
		assertEquals( 1, counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 1, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );

		assertEquals( 3, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		assertEquals( 4, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) );

		mapping.close();
		mapping = null;

		// Index manager builders must not have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );

		// All other instantiated objects must have been closed.
		assertRuntimeComponentsClosed();
	}

	@Test
	public void failingRoutingBinding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep otherIndexedEntityMapping = mappingDefinition.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.INDEX );
			otherIndexedEntityMapping.binder( StartupStubBridge.binder( TYPE_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "id" )
					.documentId()
							.identifierBinder( StartupStubBridge.binder( Integer.class, IDENTIFIER_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "text" )
					.binder( StartupStubBridge.binder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
					.genericField( "otherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractor( StartupStubContainerExtractor.NAME );
			otherIndexedEntityMapping.indexed().routingBinder( new FailingBinder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertRuntimeComponentsClosed();
	}

	@Test
	public void failingTypeBinding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep otherIndexedEntityMapping = mappingDefinition.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.INDEX )
					.routingBinder( StartupStubBridge.binder( ROUTING_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "id" )
					.documentId()
							.identifierBinder( StartupStubBridge.binder( Integer.class, IDENTIFIER_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "text" )
					.binder( StartupStubBridge.binder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
					.genericField( "otherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractor( StartupStubContainerExtractor.NAME );
			otherIndexedEntityMapping.binder( new FailingBinder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertRuntimeComponentsClosed();
	}

	@Test
	public void failingIdentifierBinding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep otherIndexedEntityMapping = mappingDefinition.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.INDEX )
					.routingBinder( StartupStubBridge.binder( ROUTING_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.binder( StartupStubBridge.binder( TYPE_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "text" )
					.binder( StartupStubBridge.binder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
					.genericField( "otherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractor( StartupStubContainerExtractor.NAME );
			otherIndexedEntityMapping.property( "id" )
					.documentId()
							.identifierBinder( new FailingBinder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertRuntimeComponentsClosed();
	}

	@Test
	public void failingPropertyBinding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep otherIndexedEntityMapping = mappingDefinition.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.INDEX )
					.routingBinder( StartupStubBridge.binder( ROUTING_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.binder( StartupStubBridge.binder( TYPE_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "id" )
					.documentId()
							.identifierBinder( StartupStubBridge.binder( Integer.class, IDENTIFIER_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "text" )
					.genericField( "otherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractor( StartupStubContainerExtractor.NAME )
					.binder( new FailingBinder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertRuntimeComponentsClosed();
	}

	@Test
	public void failingValueBinding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep otherIndexedEntityMapping = mappingDefinition.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.INDEX )
					.routingBinder( StartupStubBridge.binder( ROUTING_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.binder( StartupStubBridge.binder( TYPE_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "id" )
					.documentId().identifierBinder( StartupStubBridge.binder( Integer.class, IDENTIFIER_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "text" )
					.binder( StartupStubBridge.binder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
					.genericField( "otherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractor( StartupStubContainerExtractor.NAME )
					.genericField( "yetAnotherText" )
							.valueBinder( new FailingBinder() )
							.extractor( StartupStubContainerExtractor.NAME );
		} );


		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertRuntimeComponentsClosed();
	}

	@Test
	public void failingContainerExtractorBuilding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep otherIndexedEntityMapping = mappingDefinition.type( OtherIndexedEntity.class );
			otherIndexedEntityMapping.indexed().index( OtherIndexedEntity.INDEX )
					.routingBinder( StartupStubBridge.binder( ROUTING_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.binder( StartupStubBridge.binder( TYPE_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "id" )
					.documentId().identifierBinder( StartupStubBridge.binder( Integer.class, IDENTIFIER_BRIDGE_COUNTER_KEYS ) );
			otherIndexedEntityMapping.property( "text" )
					.binder( StartupStubBridge.binder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
					.genericField( "otherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractor( StartupStubContainerExtractor.NAME )
					.genericField( "yetAnotherText" )
							// The extractor returns type Object, not String
							.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
							.extractors( ContainerExtractorPath.explicitExtractors( Arrays.asList(
									StartupStubContainerExtractor.NAME, // The first one succeeds, but...
									FailingContainerExtractor.NAME // This one fails.
							) ) );
		} );


		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertRuntimeComponentsClosed();
	}

	private void failingStartup(Consumer<ProgrammaticMappingConfigurationContext> additionalMappingContributor) {
		assertThatThrownBy(
				() -> startup( additionalMappingContributor )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( SimulatedFailure.MESSAGE );
	}

	private void startup(Consumer<ProgrammaticMappingConfigurationContext> additionalMappingContributor) {
		this.mapping = setupHelper.start()
				.withConfiguration(
						builder -> {
							builder.addEntityType( IndexedEntity.class );
							builder.addEntityType( OtherIndexedEntity.class );

							ContainerExtractorConfigurationContext containerExtractorDefinition = builder.containerExtractors();
							containerExtractorDefinition.define( StartupStubContainerExtractor.NAME,
									StartupStubContainerExtractor.class,
									/*
									 * Increment static counters upon retrieval of the extractor
									 * and closing of its bean holder,
									 * so that we can check the bean holders are properly closed.
									 */
									ignored -> StartupStubContainerExtractor.create( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS ) );
							containerExtractorDefinition.define( FailingContainerExtractor.NAME, FailingContainerExtractor.class );

							ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();
							TypeMappingStep indexedEntityMapping = mappingDefinition.type( IndexedEntity.class );
							indexedEntityMapping.indexed().index( IndexedEntity.INDEX )
									.routingBinder( StartupStubBridge.binder( ROUTING_BRIDGE_COUNTER_KEYS ) );
							indexedEntityMapping.binder( StartupStubBridge.binder( TYPE_BRIDGE_COUNTER_KEYS ) );
							indexedEntityMapping.property( "id" )
									.documentId().identifierBinder(
											StartupStubBridge.binder( Integer.class, IDENTIFIER_BRIDGE_COUNTER_KEYS )
									);
							indexedEntityMapping.property( "text" )
									.genericField()
											// The extractor returns type Object, not String
											.valueBinder( StartupStubBridge.binder( Object.class, VALUE_BRIDGE_COUNTER_KEYS ) )
											.extractor( StartupStubContainerExtractor.NAME );
							indexedEntityMapping.property( "embedded" )
									.associationInverseSide(
											PojoModelPath.builder().property( "embedding" )
													.value( StartupStubContainerExtractor.NAME )
													.toValuePath()
									)
									.binder( StartupStubBridge.binder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
									/*
									 * This is important so that there are bridges that only contribute fields that are filtered out.
									 * These bridges are created to check what they want to contribute,
									 * and we test that they are properly closed.
									 */
									.indexedEmbedded()
											.includePaths( "text" );
							indexedEntityMapping.property( "otherEmbedded" )
									.associationInverseSide(
											PojoModelPath.builder().property( "otherEmbedding" )
													.value( StartupStubContainerExtractor.NAME )
													.toValuePath()
									);

							additionalMappingContributor.accept( mappingDefinition );
						}
				)
				.setup();
	}

	private void assertRuntimeComponentsClosed() {
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.holderClose ) );
		assertEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_BRIDGE_COUNTER_KEYS.holderClose ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.holderClose ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.holderClose ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.holderClose ) );
		assertEquals( 0, counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance )
				- counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.holderClose ) );
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.STOP_COUNTER_KEY ) );
	}

	public static class IndexedEntity {
		static final String INDEX = "IndexedEntity";

		private Integer id;

		private String text;

		private IndexedEntity embedded;

		private IndexedEntity otherEmbedded;

		private IndexedEntity embedding;

		private IndexedEntity otherEmbedding;

	}

	public static final class OtherIndexedEntity {
		static final String INDEX = "OtherIndexedEntity";

		private Integer id;

		private String text;

	}

	private static class SimulatedFailure extends RuntimeException {
		public static final String MESSAGE = "Simulated failure";
		SimulatedFailure() {
			super( MESSAGE );
		}
	}

	private static class FailingBinder implements TypeBinder, PropertyBinder,
			IdentifierBinder, ValueBinder, RoutingBinder {
		@Override
		public void bind(TypeBindingContext context) {
			throw new SimulatedFailure();
		}

		@Override
		public void bind(PropertyBindingContext context) {
			throw new SimulatedFailure();
		}

		@Override
		public void bind(IdentifierBindingContext<?> context) {
			throw new SimulatedFailure();
		}

		@Override
		public void bind(ValueBindingContext<?> context) {
			throw new SimulatedFailure();
		}

		@Override
		public void bind(RoutingBindingContext context) {
			throw new SimulatedFailure();
		}
	}

	public static class FailingContainerExtractor implements ContainerExtractor<Object, Object> {
		public static final String NAME = "failing-container-extractor";

		public FailingContainerExtractor() {
			throw new SimulatedFailure();
		}

		@Override
		public <T, C2> void extract(Object container, ValueProcessor<T, ? super Object, C2> perValueProcessor, T target,
				C2 context, ContainerExtractionContext extractionContext) {
			throw new UnsupportedOperationException( "Unexpected runtime use" );
		}
	}
}
