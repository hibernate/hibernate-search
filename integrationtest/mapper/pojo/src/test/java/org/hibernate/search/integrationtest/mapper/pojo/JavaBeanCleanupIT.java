/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMappingInitiator;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.integrationtest.mapper.pojo.bridge.StartupStubBridge;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManagerBuilder;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that all resources are closed as expected upon shutdown, or when startup fails.
 */
public class JavaBeanCleanupIT {
	private static final StartupStubBridge.CounterKeys IDENTIFIER_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys ROUTING_KEY_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys TYPE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys PROPERTY_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();
	private static final StartupStubBridge.CounterKeys VALUE_BRIDGE_COUNTER_KEYS = StartupStubBridge.createKeys();

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SearchMappingRepository mappingRepository;

	@After
	public void cleanup() {
		if ( mappingRepository != null ) {
			mappingRepository.close();
		}
	}

	@Test
	public void successfulBuilding() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );
		backendMock.expectAnySchema( OtherIndexedEntity.INDEX );

		startup( mappingDefinition -> {
			TypeMappingContext typeContext = mappingDefinition.type( OtherIndexedEntity.class );
			typeContext.indexed( OtherIndexedEntity.INDEX )
					.routingKeyBridge( new SucceedingBridgeBuilder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
					.bridge( new SucceedingBridgeBuilder( TYPE_BRIDGE_COUNTER_KEYS ) )
					.property( "id" )
							.documentId()
									.identifierBridge( new SucceedingBridgeBuilder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
					.property( "text" )
							.bridge( new SucceedingBridgeBuilder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
							.field( "otherText" )
									.valueBridge( new SucceedingBridgeBuilder( VALUE_BRIDGE_COUNTER_KEYS ) );
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

		mappingRepository.close();
		mappingRepository = null;

		// Index manager builders must not have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );

		// All other instantiated objects must have been closed.
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );
	}

	@Test
	public void failingRoutingKeyBridgeBuilding() {
		failingStartup( mappingDefinition -> {
			TypeMappingContext typeContext = mappingDefinition.type( OtherIndexedEntity.class );
			typeContext.indexed( OtherIndexedEntity.INDEX )
					.bridge( new SucceedingBridgeBuilder( TYPE_BRIDGE_COUNTER_KEYS ) )
					.property( "id" )
							.documentId()
									.identifierBridge( new SucceedingBridgeBuilder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
					.property( "text" )
							.bridge( new SucceedingBridgeBuilder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
							.field( "otherText" )
									.valueBridge( new SucceedingBridgeBuilder( VALUE_BRIDGE_COUNTER_KEYS ) );
			typeContext.routingKeyBridge( new FailingBridgeBuilder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );
	}

	@Test
	public void failingTypeBridgeBuilding() {
		failingStartup( mappingDefinition -> {
			TypeMappingContext typeContext = mappingDefinition.type( OtherIndexedEntity.class );
			typeContext.indexed( OtherIndexedEntity.INDEX )
					.routingKeyBridge( new SucceedingBridgeBuilder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
					.property( "id" )
							.documentId()
									.identifierBridge( new SucceedingBridgeBuilder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
					.property( "text" )
							.bridge( new SucceedingBridgeBuilder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
							.field( "otherText" )
									.valueBridge( new SucceedingBridgeBuilder( VALUE_BRIDGE_COUNTER_KEYS ) );
			typeContext.bridge( new FailingBridgeBuilder() );
		} );

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );
	}

	@Test
	public void failingIdentifierBridgeBuilding() {
		failingStartup( mappingDefinition -> mappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.routingKeyBridge( new SucceedingBridgeBuilder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.bridge( new SucceedingBridgeBuilder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.bridge( new SucceedingBridgeBuilder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
						.field( "otherText" )
								.valueBridge( new SucceedingBridgeBuilder( VALUE_BRIDGE_COUNTER_KEYS ) )
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
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );
	}

	@Test
	public void failingPropertyBridgeBuilding() {
		failingStartup( mappingDefinition -> mappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.routingKeyBridge( new SucceedingBridgeBuilder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.bridge( new SucceedingBridgeBuilder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.property( "id" )
						.documentId()
								.identifierBridge( new SucceedingBridgeBuilder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.field( "otherText" )
								.valueBridge( new SucceedingBridgeBuilder( VALUE_BRIDGE_COUNTER_KEYS ) )
						.bridge( new FailingBridgeBuilder() )
		);

		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );
	}

	@Test
	public void failingValueBridgeBuilding() {
		failingStartup( mappingDefinition -> mappingDefinition.type( OtherIndexedEntity.class )
				.indexed( OtherIndexedEntity.INDEX )
				.routingKeyBridge( new SucceedingBridgeBuilder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.bridge( new SucceedingBridgeBuilder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.property( "id" )
						.documentId().identifierBridge( new SucceedingBridgeBuilder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.bridge( new SucceedingBridgeBuilder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
						.field( "otherText" ).valueBridge( new FailingBridgeBuilder() )
		);


		// We must have instantiated objects...
		assertEquals( 2, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY ) );
		assertNotEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance ) );
		assertNotEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance ) );
		// ... except index managers...
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY ) );

		// ... and all instantiated objects must have been closed.
		assertEquals( 0, counters.get( StubIndexManagerBuilder.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManagerBuilder.CLOSE_ON_FAILURE_COUNTER_KEY ) );
		assertEquals( 0, counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( IDENTIFIER_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( ROUTING_KEY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( TYPE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( TYPE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( PROPERTY_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( VALUE_BRIDGE_COUNTER_KEYS.instance )
				- counters.get( VALUE_BRIDGE_COUNTER_KEYS.close ) );
		assertEquals( 0, counters.get( StubIndexManager.INSTANCE_COUNTER_KEY )
				- counters.get( StubIndexManager.CLOSE_COUNTER_KEY ) );
	}

	private void failingStartup(Consumer<ProgrammaticMappingDefinition> additionalMappingContributor) {
		SubTest.expectException(
				() -> startup( additionalMappingContributor )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( SimulatedFailure.MESSAGE );
	}

	private void startup(Consumer<ProgrammaticMappingDefinition> additionalMappingContributor) {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.setProperty( "index.default.backend", "stubBackend" );

		JavaBeanMappingInitiator initiator = JavaBeanMappingInitiator.create( mappingRepositoryBuilder );

		initiator.addEntityType( IndexedEntity.class );
		initiator.addEntityType( OtherIndexedEntity.class );

		ProgrammaticMappingDefinition mappingDefinition = initiator.programmaticMapping();
		mappingDefinition.type( IndexedEntity.class )
				.indexed( IndexedEntity.INDEX )
				.bridge( new SucceedingBridgeBuilder( TYPE_BRIDGE_COUNTER_KEYS ) )
				.routingKeyBridge( new SucceedingBridgeBuilder( ROUTING_KEY_BRIDGE_COUNTER_KEYS ) )
				.property( "id" )
						.documentId().identifierBridge( new SucceedingBridgeBuilder( IDENTIFIER_BRIDGE_COUNTER_KEYS ) )
				.property( "text" )
						.field().valueBridge( new SucceedingBridgeBuilder( VALUE_BRIDGE_COUNTER_KEYS ) )
				.property( "embedded" )
						.associationInverseSide(
								PojoModelPath.fromRoot( "embedding" )
										.value( ContainerValueExtractorPath.defaultExtractors() )
						)
						.bridge( new SucceedingBridgeBuilder( PROPERTY_BRIDGE_COUNTER_KEYS ) )
						/*
						 * This is important so that there are bridges that only contribute fields that are filtered out.
						 * These bridges are created to check what they want to contribute,
						 * and we test that they are properly closed.
						 */
						.indexedEmbedded()
								.includePaths( "text" )
				.property( "otherEmbedded" )
						.associationInverseSide(
								PojoModelPath.fromRoot( "otherEmbedding" )
										.value( ContainerValueExtractorPath.defaultExtractors() )
						);

		additionalMappingContributor.accept( mappingDefinition );

		mappingRepository = mappingRepositoryBuilder.build();
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

		public void setOtherEmbedding(
				IndexedEntity otherEmbedding) {
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

	private static class FailingBridgeBuilder implements BridgeBuilder<StartupStubBridge> {
		@Override
		public StartupStubBridge build(BridgeBuildContext buildContext) {
			throw new SimulatedFailure();
		}
	}

	private static class SucceedingBridgeBuilder implements BridgeBuilder<StartupStubBridge> {
		private final StartupStubBridge.CounterKeys counterKeys;

		private SucceedingBridgeBuilder(StartupStubBridge.CounterKeys counterKeys) {
			this.counterKeys = counterKeys;
		}

		@Override
		public StartupStubBridge build(BridgeBuildContext buildContext) {
			return new StartupStubBridge( counterKeys );
		}
	}
}
