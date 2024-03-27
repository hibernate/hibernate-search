/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Consumer;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubBridge;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubContainerExtractor;
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
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManagerBuilder;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test that all resources are closed as expected upon shutdown, or when startup fails.
 */
class CleanupIT {
	private static final StartupStubBridge.CounterKeys IDENTIFIER_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys ROUTING_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys TYPE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys PROPERTY_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubContainerExtractor.CounterKeys CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS =
			StartupStubContainerExtractor.createKeys();
	private static final StartupStubBridge.CounterKeys VALUE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@RegisterExtension
	public StaticCounters counters = StaticCounters.create();

	private CloseableSearchMapping mapping;

	@Test
	void successfulBuilding() {
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
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( StubIndexManager.STOP_COUNTER_KEY ) ).isZero();

		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) ).isEqualTo( 2 );
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) ).isEqualTo( 2 );

		// Extra type and property bridges should have been created...
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) ).isEqualTo( 2 + 1 );
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) ).isEqualTo( 2 + 1 );
		// ... but closed immediately because they were only contributing fields that were filtered out
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) ).isEqualTo( 1 );
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) ).isEqualTo( 1 );

		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) ).isEqualTo( 3 );
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) ).isEqualTo( 4 );

		mapping.close();
		mapping = null;

		// Index manager builders must not have been closed.
		assertThat( counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();

		// All other instantiated objects must have been closed.
		assertRuntimeComponentsClosed();
	}

	@Test
	void failingRoutingBinding() {
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
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) ).isNotZero();
		// ... except index managers...
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) ).isZero();

		// ... and all instantiated objects must have been closed.
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();
		assertRuntimeComponentsClosed();
	}

	@Test
	void failingTypeBinding() {
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
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) ).isNotZero();
		// ... except index managers...
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) ).isZero();

		// ... and all instantiated objects must have been closed.
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();
		assertRuntimeComponentsClosed();
	}

	@Test
	void failingIdentifierBinding() {
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
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) ).isNotZero();
		// ... except index managers...
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) ).isZero();

		// ... and all instantiated objects must have been closed.
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();
		assertRuntimeComponentsClosed();
	}

	@Test
	void failingPropertyBinding() {
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
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) ).isNotZero();
		// ... except index managers...
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) ).isZero();

		// ... and all instantiated objects must have been closed.
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();
		assertRuntimeComponentsClosed();
	}

	@Test
	void failingValueBinding() {
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
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) ).isNotZero();
		// ... except index managers...
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) ).isZero();

		// ... and all instantiated objects must have been closed.
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();
		assertRuntimeComponentsClosed();
	}

	@Test
	void failingContainerExtractorBuilding() {
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
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) ).isEqualTo( 2 );
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) ).isNotZero();
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance ) ).isNotZero();
		// ... except index managers...
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) ).isZero();

		// ... and all instantiated objects must have been closed.
		assertThat( counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) ).isZero();
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
							ContainerExtractorConfigurationContext containerExtractorDefinition = builder.containerExtractors();
							containerExtractorDefinition.define( StartupStubContainerExtractor.NAME,
									StartupStubContainerExtractor.class,
									/*
									 * Increment static counters upon retrieval of the extractor
									 * and closing of its bean holder,
									 * so that we can check the bean holders are properly closed.
									 */
									ignored -> StartupStubContainerExtractor.create( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS ) );
							containerExtractorDefinition.define( FailingContainerExtractor.NAME,
									FailingContainerExtractor.class );

							ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();
							TypeMappingStep indexedEntityMapping = mappingDefinition.type( IndexedEntity.class );
							indexedEntityMapping.searchEntity();
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

							mappingDefinition.type( OtherIndexedEntity.class )
									.searchEntity();

							additionalMappingContributor.accept( mappingDefinition );
						}
				)
				.setup();
	}

	private void assertRuntimeComponentsClosed() {
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) ).isZero();
		assertThat( counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.holderClose ) ).isZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_BRIDGE_COUNTER_KEYS.close ) ).isZero();
		assertThat( counters.get( ROUTING_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_BRIDGE_COUNTER_KEYS.holderClose ) ).isZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) ).isZero();
		assertThat( counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.holderClose ) ).isZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) ).isZero();
		assertThat( counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.holderClose ) ).isZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) ).isZero();
		assertThat( counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.holderClose ) ).isZero();
		assertThat( counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.instance )
				- counters.get( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS.holderClose ) ).isZero();
		assertThat( counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.STOP_COUNTER_KEY ) ).isZero();
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

	private static class FailingBinder
			implements TypeBinder, PropertyBinder,
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
