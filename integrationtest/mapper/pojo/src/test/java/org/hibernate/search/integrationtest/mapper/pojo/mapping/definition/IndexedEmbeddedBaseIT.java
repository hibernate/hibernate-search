/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.junit.Assert.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.AnnotationMappingSmokeIT;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.ProgrammaticMappingSmokeIT;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubBridge;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.GeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LatitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@code @IndexedEmbedded} annotation.
 * <p>
 * Does not test all the corner cases of filtering when using {@link IndexedEmbedded#includePaths()} and
 * {@link IndexedEmbedded#maxDepth()}, which are tested in a unit test in the engine module
 * (the test is named {@code ConfiguredIndexSchemaNestingContextTest} at the time of this writing).
 * <p>
 * Does not test uses of container value extractors (for now). Some of them are tested in
 * {@link AnnotationMappingSmokeIT} and {@link ProgrammaticMappingSmokeIT}.
 */
@SuppressWarnings("unused")
public class IndexedEmbeddedBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	@Test
	public void noParameter() {
		class IndexedEmbeddedLevel2 {
			String level2Property;
			@GenericField
			public String getLevel2Property() {
				return level2Property;
			}
		}
		class IndexedEmbeddedLevel1 {
			String level1Property;
			IndexedEmbeddedLevel2 level2;
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
			@IndexedEmbedded
			public IndexedEmbeddedLevel2 getLevel2() {
				return level2;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value, String level2Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
				this.level1.level2 = new IndexedEmbeddedLevel2();
				this.level1.level2.level2Property = level2Value;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded
			public IndexedEmbeddedLevel1 getLevel1() {
				return level1;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1Property", String.class )
						.objectField( "level2", b3 -> b3
								.field( "level2Property", String.class )
						)
				)
		);
		JavaBeanMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class, IndexedEmbeddedLevel2.class )
				.setup();
		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> new IndexedEntity( id, "level1Value", "level2Value" ),
				document -> document.objectField( "level1", b2 -> b2
						.field( "level1Property", "level1Value" )
						.objectField( "level2", b3 -> b3
								.field( "level2Property", "level2Value" )
						)
				)
		);
	}

	/**
	 * Check that setting a dotless prefix in @IndexedEmbedded on a multi-valued property
	 * results in *direct* children being automatically marked as multi-valued.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3324")
	public void prefix_multiValued() {
		class IndexedEmbeddedLevel2 {
			String level2Property;
			@GenericField
			public String getLevel2Property() {
				return level2Property;
			}
		}
		class IndexedEmbeddedLevel1 {
			IndexedEmbeddedLevel2 level2NoDotInPrefix = new IndexedEmbeddedLevel2();
			IndexedEmbeddedLevel2 level2OneDotInPrefix = new IndexedEmbeddedLevel2();
			IndexedEmbeddedLevel2 level2TwoDotsInPrefix = new IndexedEmbeddedLevel2();
			String level1Property;
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
			@IndexedEmbedded(prefix = "level2NoDotInPrefix_")
			public IndexedEmbeddedLevel2 getLevel2NoDotInPrefix() {
				return level2NoDotInPrefix;
			}
			@IndexedEmbedded(prefix = "level2OneDotInPrefix.")
			public IndexedEmbeddedLevel2 getLevel2OneDotInPrefix() {
				return level2OneDotInPrefix;
			}
			@IndexedEmbedded(prefix = "level2TwoDotsInPrefix.level3.")
			public IndexedEmbeddedLevel2 getLevel2TwoDotsInPrefix() {
				return level2TwoDotsInPrefix;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			List<IndexedEmbeddedLevel1> level1 = new ArrayList<>();
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded(prefix = "level1_")
			public List<IndexedEmbeddedLevel1> getLevel1() {
				return level1;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "level1_level1Property", String.class, b2 -> b2.multiValued( true ) )
				.field( "level1_level2NoDotInPrefix_level2Property", String.class, b2 -> b2.multiValued( true ) )
				.objectField( "level1_level2OneDotInPrefix", ObjectFieldStorage.DEFAULT, b2 -> b2
						.multiValued( true )
						// Not a direct child of level1: should be single-valued
						.field( "level2Property", String.class )
				)
				.objectField( "level1_level2TwoDotsInPrefix", ObjectFieldStorage.DEFAULT, b2 -> b2
						.multiValued( true )
						// Not a direct child of level1: should be single-valued
						.objectField( "level3", ObjectFieldStorage.DEFAULT, b3 -> b3
								// Not a direct child of level1: should be single-valued
								.field( "level2Property", String.class )
						)
				)
		);
		JavaBeanMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
				.setup();
		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					IndexedEmbeddedLevel1 level1_1 = new IndexedEmbeddedLevel1();
					level1_1.level1Property = "1";
					level1_1.level2NoDotInPrefix.level2Property = "1";
					level1_1.level2OneDotInPrefix.level2Property = "1";
					level1_1.level2TwoDotsInPrefix.level2Property = "1";
					entity.level1.add( level1_1 );
					IndexedEmbeddedLevel1 level1_2 = new IndexedEmbeddedLevel1();
					level1_2.level1Property = "2";
					level1_2.level2NoDotInPrefix.level2Property = "2";
					level1_2.level2OneDotInPrefix.level2Property = "2";
					level1_2.level2TwoDotsInPrefix.level2Property = "2";
					entity.level1.add( level1_2 );
					return entity;
				},
				document -> document
						.field( "level1_level1Property", "1", "2" )
						.field( "level1_level2NoDotInPrefix_level2Property", "1", "2" )
						.objectField( "level1_level2OneDotInPrefix", b2 -> b2
								.field( "level2Property", "1" )
						)
						.objectField( "level1_level2OneDotInPrefix", b2 -> b2
								.field( "level2Property", "2" )
						)
						.objectField( "level1_level2TwoDotsInPrefix", b2 -> b2
								.objectField( "level3", b3 -> b3
										.field( "level2Property", "1" )
								)
						)
						.objectField( "level1_level2TwoDotsInPrefix", b2 -> b2
								.objectField( "level3", b3 -> b3
										.field( "level2Property", "2" )
								)
						)
		);
	}

	/**
	 * Check that the "prefix" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void prefix() {
		class IndexedEmbeddedLevel1 {
			String level1Property;
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Property) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Property;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded(prefix = "customPrefix_")
			public IndexedEmbeddedLevel1 getLevel1() {
				return level1;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "customPrefix_level1Property", String.class )
		);
		JavaBeanMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
				.setup();
		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> new IndexedEntity( id, "level1Value" ),
				document -> document
						.field( "customPrefix_level1Property", "level1Value" )
		);
	}

	/**
	 * Check that the "includePaths" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void includePaths() {
		class IndexedEmbeddedLevel1 {
			String ignoredProperty;
			String includedProperty;
			@GenericField
			public String getIgnoredProperty() {
				return ignoredProperty;
			}
			@GenericField
			public String getIncludedProperty() {
				return includedProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String ignoredProperty, String includedProperty) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.ignoredProperty = ignoredProperty;
				this.level1.includedProperty = includedProperty;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded(includePaths = "includedProperty")
			public IndexedEmbeddedLevel1 getLevel1() {
				return level1;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "includedProperty", String.class )
				)
		);
		JavaBeanMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
				.setup();
		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> new IndexedEntity( id, "valueForIgnored", "valueForIncluded" ),
				document -> document.objectField( "level1", b2 -> b2
						.field( "includedProperty", "valueForIncluded" )
				)
		);
	}

	/**
	 * Check that an "includePaths" parameter that doesn't match anything is reported to the user.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3136")
	public void error_includePaths_nonMatched() {
		class IndexedEmbeddedLevel1 {
			String ignoredProperty;
			String includedProperty;
			@GenericField
			public String getIgnoredProperty() {
				return ignoredProperty;
			}
			@GenericField
			public String getIncludedProperty() {
				return includedProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String ignoredProperty, String includedProperty) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.ignoredProperty = ignoredProperty;
				this.level1.includedProperty = includedProperty;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded(includePaths = {"includedProperty", "nonMatchingPath"})
			public IndexedEmbeddedLevel1 getLevel1() {
				return level1;
			}
		}

		SubTest.expectException(
				() -> setupHelper.start()
						.withAnnotatedEntityTypes( IndexedEntity.class )
						.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
						.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".level1" )
						.failure(
								"IndexedEmbedded defines includePaths filters that do not match anything",
								"Non-matching includePaths filters:",
								CollectionHelper.asLinkedHashSet( "nonMatchingPath" ).toString(),
								"Encountered field paths:",
								CollectionHelper.asLinkedHashSet( "ignoredProperty", "includedProperty" ).toString(),
								"Check the filters for typos, or remove them if they are not useful"
						)
						.build()
				);
	}

	/**
	 * Check that the "maxDepth" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void maxDepth() {
		class IndexedEmbeddedLevel2 {
			String level2Property;
			@GenericField
			public String getLevel2Property() {
				return level2Property;
			}
		}
		class IndexedEmbeddedLevel1 {
			String level1Property;
			IndexedEmbeddedLevel2 level2;
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
			@IndexedEmbedded
			public IndexedEmbeddedLevel2 getLevel2() {
				return level2;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value, String level2Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
				this.level1.level2 = new IndexedEmbeddedLevel2();
				this.level1.level2.level2Property = level2Value;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded(maxDepth = 1)
			public IndexedEmbeddedLevel1 getLevel1() {
				return level1;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		JavaBeanMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class, IndexedEmbeddedLevel2.class )
				.setup();
		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> new IndexedEntity( id, "level1Value", "level2Value" ),
				document -> document.objectField( "level1", b2 -> b2
						.field( "level1Property", "level1Value" )
				)
		);
	}

	/**
	 * Check that the "storage" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void storage() {
		class IndexedEmbeddedLevel1 {
			String level1Property;
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
			}
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded(storage = ObjectFieldStorage.NESTED)
			public IndexedEmbeddedLevel1 getLevel1() {
				return level1;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", ObjectFieldStorage.NESTED, b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		JavaBeanMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
				.setup();
		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> new IndexedEntity( id, "level1Value" ),
				document -> document.objectField( "level1", b2 -> b2
						.field( "level1Property", "level1Value" )
				)
		);
	}

	/**
	 * Check that bridges whose contributed fields are all filtered out are never applied.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3212", "HSEARCH-3213" })
	public void includePaths_excludesBridges() {
		StaticCounters.Key getLongitudeKey = StaticCounters.createKey();
		StaticCounters.Key getLatitudeKey = StaticCounters.createKey();

		class IndexedEmbeddedLevel1 {
			String level1Property;
			public String getLevel1Property() {
				return level1Property;
			}
			public Double getLongitude() {
				StaticCounters.get().increment( getLongitudeKey );
				return null;
			}
			public Double getLatitude() {
				StaticCounters.get().increment( getLatitudeKey );
				return null;
			}
		}
		class IndexedEntity {
			Integer id;
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Property) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Property;
			}
			public Integer getId() {
				return id;
			}
			public IndexedEmbeddedLevel1 getLevel1() {
				return level1;
			}
		}

		StartupStubBridge.CounterKeys filteredOutBridgeCounterKeys = StartupStubBridge.createKeys();

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1IncludedField", String.class )
				)
		);
		JavaBeanMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					b.addEntityType( IndexedEntity.class );
					b.programmaticMapping().type( IndexedEntity.class )
							.indexed( INDEX_NAME )
							.property( "id" )
									.documentId()
							.property( "level1" )
									.indexedEmbedded()
											.includePaths( "level1IncludedField" );
					b.programmaticMapping().type( IndexedEmbeddedLevel1.class )
							.binder( StartupStubBridge.binder( filteredOutBridgeCounterKeys ) )
							.binder( new GeoPointBridge.Binder().fieldName( "location" ) )
							.property( "latitude" ).marker( new LatitudeMarker.Binder() )
							.property( "longitude" ).marker( new LongitudeMarker.Binder() )
							.property( "level1Property" )
									.binder( StartupStubBridge.binder( filteredOutBridgeCounterKeys ) )
									.genericField( "level1IncludedField" )
									.genericField( "filteredOut" )
											.valueBridge( StartupStubBridge.binder( String.class, filteredOutBridgeCounterKeys ) );
				} )
				.setup();
		backendMock.verifyExpectationsMet();

		/*
		 * All the bridges that were filtered out should have been instantiated,
		 * but then immediately closed.
		 * We can't check the GeoPoint bridge here, because it doesn't use static counters
		 * like our stub bridges, but we will check it isn't executed below.
		 */
		assertEquals( 3, counters.get( filteredOutBridgeCounterKeys.instance ) );
		assertEquals( 0, counters.get( filteredOutBridgeCounterKeys.instance )
				- counters.get( filteredOutBridgeCounterKeys.close ) );
		assertEquals( 0, counters.get( filteredOutBridgeCounterKeys.instance )
				- counters.get( filteredOutBridgeCounterKeys.holderClose ) );

		doTestEmbeddedRuntime(
				mapping,
				id -> new IndexedEntity( id, "level1Value" ),
				document -> document.objectField( "level1", b2 -> b2
						.field( "level1IncludedField", "level1Value" )
				)
		);

		// The bridges that were filtered out should not have been used.
		assertEquals( 0, counters.get( filteredOutBridgeCounterKeys.runtimeUse ) );
		assertEquals( 0, counters.get( getLatitudeKey ) );
		assertEquals( 0, counters.get( getLongitudeKey ) );
	}

	private <E> void doTestEmbeddedRuntime(JavaBeanMapping mapping,
			Function<Integer, E> newEntityFunction,
			Consumer<StubDocumentNode.Builder> expectedDocumentContributor) {
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = newEntityFunction.apply( 1 );

			session.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", expectedDocumentContributor )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}
}
