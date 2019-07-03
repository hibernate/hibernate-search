/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubContainerExtractor;
import org.hibernate.search.mapper.javabean.CloseableJavaBeanMapping;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubBridge;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManagerBuilder;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test that all resources are closed as expected upon shutdown, or when startup fails.
 */
public class CleanupIT {
	private static final StartupStubBridge.CounterKeys IDENTIFIER_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys ROUTING_KEY_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys TYPE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys PROPERTY_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubContainerExtractor.CounterKeys CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS =
			StartupStubContainerExtractor.createKeys();
	private static final StartupStubBridge.CounterKeys VALUE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private CloseableJavaBeanMapping mapping;

	@Test
	public void successfulBuilding() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );
		backendMock.expectAnySchema( OtherIndexedEntity.INDEX );

		startup( mappingDefinition -> {
			TypeMappingStep typeContext = mappingDefinition.type( OtherIndexedEntity.class );
			typeContext.indexed( OtherIndexedEntity.INDEX )
					.routingKeyBridge( StartupStubBridge.builder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
					.bridge( StartupStubBridge.builder( TYPE_BRIDGE_COUNTER_KEYS ) )
					.property( "id" )
							.documentId()
									.identifierBridge( StartupStubBridge.builder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
					.property( "text" )
							.bridge( StartupStubBridge.builder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
							.genericField( "otherText" )
									.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
									.withExtractor( StartupStubContainerExtractor.NAME );
		} );

		backendMock.verifyExpectationsMet();

		// Exactly 2 index managers are expected
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertEquals( 2, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );

		assertEquals( 2, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertEquals( 2, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );

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
	public void failingRoutingKeyBridgeBuilding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep typeContext = mappingDefinition.type( OtherIndexedEntity.class );
			typeContext.indexed( OtherIndexedEntity.INDEX )
					.bridge( StartupStubBridge.builder( TYPE_BRIDGE_COUNTER_KEYS ) )
					.property( "id" )
							.documentId()
									.identifierBridge( StartupStubBridge.builder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
					.property( "text" )
							.bridge( StartupStubBridge.builder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
							.genericField( "otherText" )
									.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
									.withExtractor( StartupStubContainerExtractor.NAME );
			typeContext.routingKeyBridge( new FailingBridgeBuilder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
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
	public void failingTypeBridgeBuilding() {
		failingStartup( mappingDefinition -> {
			TypeMappingStep typeContext = mappingDefinition.type( OtherIndexedEntity.class );
			typeContext.indexed( OtherIndexedEntity.INDEX )
					.routingKeyBridge( StartupStubBridge.builder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
					.property( "id" )
							.documentId()
									.identifierBridge( StartupStubBridge.builder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
					.property( "text" )
							.bridge( StartupStubBridge.builder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
							.genericField( "otherText" )
									.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
									.withExtractor( StartupStubContainerExtractor.NAME );
			typeContext.bridge( new FailingBridgeBuilder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
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
	public void failingIdentifierBridgeBuilding() {
		failingStartup( mappingDefinition -> mappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.routingKeyBridge( StartupStubBridge.builder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.bridge( StartupStubBridge.builder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.bridge( StartupStubBridge.builder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
						.genericField( "otherText" )
								.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
								.withExtractor( StartupStubContainerExtractor.NAME )
				.property( "id" )
						.documentId()
								.identifierBridge( new FailingBridgeBuilder() )
		);

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
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
	public void failingPropertyBridgeBuilding() {
		failingStartup( mappingDefinition -> mappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.routingKeyBridge( StartupStubBridge.builder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.bridge( StartupStubBridge.builder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.property( "id" )
						.documentId()
								.identifierBridge( StartupStubBridge.builder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.genericField( "otherText" )
								.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
								.withExtractor( StartupStubContainerExtractor.NAME )
						.bridge( new FailingBridgeBuilder() )
		);

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
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
	public void failingValueBridgeBuilding() {
		failingStartup( mappingDefinition -> mappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.routingKeyBridge( StartupStubBridge.builder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.bridge( StartupStubBridge.builder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.property( "id" )
						.documentId().identifierBridge( StartupStubBridge.builder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.bridge( StartupStubBridge.builder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
						.genericField( "otherText" )
								.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
								.withExtractor( StartupStubContainerExtractor.NAME )
						.genericField( "yetAnotherText" )
								.valueBridge( new FailingBridgeBuilder() )
								.withExtractor( StartupStubContainerExtractor.NAME )
		);


		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
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
		failingStartup( mappingDefinition -> mappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.routingKeyBridge( StartupStubBridge.builder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.bridge( StartupStubBridge.builder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.property( "id" )
						.documentId().identifierBridge( StartupStubBridge.builder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.bridge( StartupStubBridge.builder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
						.genericField( "otherText" )
								.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
								.withExtractor( StartupStubContainerExtractor.NAME )
						.genericField( "yetAnotherText" )
								.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
								.withExtractors( ContainerExtractorPath.explicitExtractors( Arrays.asList(
										StartupStubContainerExtractor.NAME, // The first one succeeds, but...
										FailingContainerExtractor.NAME // This one fails.
								) ) )
		);


		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
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
		SubTest.expectException(
				() -> startup( additionalMappingContributor )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( SimulatedFailure.MESSAGE );
	}

	private void startup(Consumer<ProgrammaticMappingConfigurationContext> additionalMappingContributor) {
		this.mapping = setupHelper.start()
				/*
				 * Make the StartubStubContainerExtractor available through a factory
				 * that will return a BeanHolder that increments static counters
				 * (so that we can check the bean holders are properly closed).
				 */
				.withProperty( EngineSpiSettings.BEAN_CONFIGURERS, Arrays.asList(
						new BeanConfigurer() {
							@Override
							public void configure(BeanConfigurationContext context) {
								context.define(
										StartupStubContainerExtractor.class,
										creationContext ->
												StartupStubContainerExtractor.create( CONTAINER_VALUE_EXTRACTOR_COUNTER_KEYS )
								);
							}
						}
				) )
				.withConfiguration(
						builder -> {
							builder.addEntityType( IndexedEntity.class );
							builder.addEntityType( OtherIndexedEntity.class );

							ContainerExtractorConfigurationContext containerExtractorDefinition = builder.containerExtractors();
							containerExtractorDefinition.define( StartupStubContainerExtractor.NAME, StartupStubContainerExtractor.class );
							containerExtractorDefinition.define( FailingContainerExtractor.NAME, FailingContainerExtractor.class );

							ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();
							mappingDefinition.type( IndexedEntity.class )
									.indexed( IndexedEntity.INDEX )
									.bridge( StartupStubBridge.builder( TYPE_BRIDGE_COUNTER_KEYS ) )
									.routingKeyBridge( StartupStubBridge.builder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
									.property( "id" )
											.documentId().identifierBridge(
													StartupStubBridge.builder( IDENTIFIER_BRIDGE_COUNTER_KEYS )
											)
									.property( "text" )
											.genericField()
													.valueBridge( StartupStubBridge.builder( VALUE_BRIDGE_COUNTER_KEYS ) )
													.withExtractor( StartupStubContainerExtractor.NAME )
									.property( "embedded" )
											.associationInverseSide(
													PojoModelPath.builder().property( "embedding" )
															.value( StartupStubContainerExtractor.NAME )
															.toValuePath()
											)
											.bridge( StartupStubBridge.builder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
											/*
											 * This is important so that there are bridges that only contribute fields that are filtered out.
											 * These bridges are created to check what they want to contribute,
											 * and we test that they are properly closed.
											 */
											.indexedEmbedded()
													.includePaths( "text" )
									.property( "otherEmbedded" )
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
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.holderClose ) );
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
				- counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );
	}

	public static class IndexedEntity {
		static final String INDEX = "IndexedEntity";

		private Integer id;

		private String text;

		private IndexedEntity embedded;

		private IndexedEntity otherEmbedded;

		private IndexedEntity embedding;

		private IndexedEntity otherEmbedding;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public IndexedEntity getEmbedded() {
			return embedded;
		}

		public void setEmbedded(IndexedEntity embedded) {
			this.embedded = embedded;
		}

		public IndexedEntity getOtherEmbedded() {
			return otherEmbedded;
		}

		public void setOtherEmbedded(IndexedEntity otherEmbedded) {
			this.otherEmbedded = otherEmbedded;
		}

		public IndexedEntity getEmbedding() {
			return embedding;
		}

		public void setEmbedding(IndexedEntity embedding) {
			this.embedding = embedding;
		}

		public IndexedEntity getOtherEmbedding() {
			return otherEmbedding;
		}

		public void setOtherEmbedding(IndexedEntity otherEmbedding) {
			this.otherEmbedding = otherEmbedding;
		}
	}

	public static final class OtherIndexedEntity {
		static final String INDEX = "OtherIndexedEntity";

		private Integer id;

		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		public static final String MESSAGE = "Simulated failure";
		SimulatedFailure() {
			super( MESSAGE );
		}
	}

	private static class FailingBridgeBuilder implements TypeBridgeBuilder<Annotation>,
			PropertyBridgeBuilder<Annotation>, RoutingKeyBridgeBuilder<Annotation>,
			IdentifierBridgeBuilder, ValueBridgeBuilder {
		@Override
		public void initialize(Annotation annotation) {
			// Nothing to do
		}

		@Override
		public BeanHolder<? extends IdentifierBridge<?>> buildForIdentifier(BridgeBuildContext buildContext) {
			throw new SimulatedFailure();
		}

		@Override
		public BeanHolder<? extends PropertyBridge> buildForProperty(BridgeBuildContext buildContext) {
			throw new SimulatedFailure();
		}

		@Override
		public BeanHolder<? extends RoutingKeyBridge> buildForRoutingKey(BridgeBuildContext buildContext) {
			throw new SimulatedFailure();
		}

		@Override
		public BeanHolder<? extends TypeBridge> buildForType(BridgeBuildContext buildContext) {
			throw new SimulatedFailure();
		}

		@Override
		public void bind(ValueBindingContext<?> context) {
			throw new SimulatedFailure();
		}
	}

	public static class FailingContainerExtractor implements ContainerExtractor<Object, Object> {
		public static final String NAME = "failing-container-extractor";

		public FailingContainerExtractor() {
			throw new SimulatedFailure();
		}

		@Override
		public Stream<Object> extract(Object container) {
			throw new UnsupportedOperationException( "Unexpected runtime use" );
		}
	}

}
