/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.AnnotationMappingSmokeIT;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.ProgrammaticMappingSmokeIT;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.StartupStubBridge;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.GeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LatitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@code @IndexedEmbedded} annotation.
 * <p>
 * Does not test all the corner cases of filtering when using {@link IndexedEmbedded#includePaths()} and
 * {@link IndexedEmbedded#includeDepth()}, which are tested in a unit test in the engine module
 * (the test is named {@code ConfiguredIndexSchemaNestingContextTest} at the time of this writing).
 * <p>
 * Does not test uses of container value extractors (for now). Some of them are tested in
 * {@link AnnotationMappingSmokeIT} and {@link ProgrammaticMappingSmokeIT}.
 */
@SuppressWarnings("unused")
public class IndexedEmbeddedBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	@Test
	public void defaultAttributes() {
		class IndexedEmbeddedLevel2 {
			@GenericField
			String level2Property;
		}
		class IndexedEmbeddedLevel1 {
			@GenericField
			String level1Property;
			@IndexedEmbedded
			IndexedEmbeddedLevel2 level2;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value, String level2Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
				this.level1.level2 = new IndexedEmbeddedLevel2();
				this.level1.level2.level2Property = level2Value;
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
		SearchMapping mapping = setupHelper.start()
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

	@Test
	public void name() {
		class IndexedEmbeddedLevel1 {
			@GenericField
			String level1Property;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(name = "explicitName")
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "explicitName", b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
				.setup();
		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> new IndexedEntity( id, "level1Value" ),
				document -> document
						.objectField( "explicitName", b2 -> b2
								.field( "level1Property", "level1Value" )
				)
		);
	}

	@Test
	public void name_invalid_dot() {
		class IndexedEmbeddedLevel1 {
			@GenericField
			String level1Property;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(name = "invalid.withdot")
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = value;
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".level1" )
						.annotationContextAnyParameters( IndexedEmbedded.class )
						.failure(
								"Index field name 'invalid.withdot' is invalid: field names cannot contain a dot ('.')"
						)
						.build()
				);
	}

	@Test
	public void name_andPrefix() {
		class IndexedEmbeddedLevel1 {
			@GenericField
			String level1Property;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(name = "somename", prefix = "someprefix.")
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = value;
			}
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".level1" )
						.annotationContextAnyParameters( IndexedEmbedded.class )
						.failure(
								"Cannot set both the name and prefix in @IndexedEmbedded",
								"Name was 'somename', prefix was 'someprefix.'."
						)
						.build()
				);
	}

	@Test
	public void repeatedIndexedEmbedded() {
		class Embedded {
			String forDefault;
			String flat;
			String nest;
			String common;

			@GenericField(name = "default")
			public String getForDefault() {
				return forDefault;
			}
			@GenericField
			public String getFlat() {
				return flat;
			}
			@GenericField
			public String getNest() {
				return nest;
			}
			@GenericField
			public String getCommon() {
				return common;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(name = "default", includePaths = {"default", "common"})
			@IndexedEmbedded(name = "flat", includePaths = {"flat", "common"},
					structure = ObjectStructure.FLATTENED)
			@IndexedEmbedded(name = "nest", includePaths = {"nest", "common"},
					structure = ObjectStructure.NESTED)
			Embedded embedded;
			public IndexedEntity(int id, String value) {
				this.id = id;
				this.embedded = new Embedded();
				this.embedded.forDefault = value;
				this.embedded.flat = value;
				this.embedded.nest = value;
				this.embedded.common = value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> {
				b.objectField( "default", ObjectStructure.DEFAULT, b2 -> {
					b2.field( "default", String.class );
					b2.field( "common", String.class );
				} );
				b.objectField( "flat", ObjectStructure.FLATTENED, b2 -> {
					b2.field( "flat", String.class );
					b2.field( "common", String.class );
				} );
				b.objectField( "nest", ObjectStructure.NESTED, b2 -> {
					b2.field( "nest", String.class );
					b2.field( "common", String.class );
				} );
			}
		);
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( Embedded.class )
				.setup();
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Check @IndexedEmbedded on a multi-valued property
	 * results in the corresponding object field being automatically marked as multi-valued
	 * (and not its own fields).
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3324")
	public void multiValued() {
		class IndexedEmbeddedLevel1 {
			@GenericField
			String level1SingleValuedProperty;
			@GenericField
			List<String> level1MultiValuedProperty;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded
			List<IndexedEmbeddedLevel1> level1 = new ArrayList<>();
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", ObjectStructure.DEFAULT, b2 -> b2
						.multiValued( true )
						.field( "level1SingleValuedProperty", String.class )
						.field( "level1MultiValuedProperty", String.class, b3 -> b3.multiValued( true ) )
				)
		);
		SearchMapping mapping = setupHelper.start()
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
					level1_1.level1SingleValuedProperty = "1";
					level1_1.level1MultiValuedProperty = Arrays.asList( "1_1", "1_2" );
					entity.level1.add( level1_1 );
					IndexedEmbeddedLevel1 level1_2 = new IndexedEmbeddedLevel1();
					level1_2.level1SingleValuedProperty = "2";
					level1_2.level1MultiValuedProperty = Arrays.asList( "2_1", "2_2" );
					entity.level1.add( level1_2 );
					return entity;
				},
				document -> document
						.objectField( "level1", b2 -> b2
								.field( "level1SingleValuedProperty", "1" )
								.field( "level1MultiValuedProperty", "1_1", "1_2" )
						)
						.objectField( "level1", b2 -> b2
								.field( "level1SingleValuedProperty", "2" )
								.field( "level1MultiValuedProperty", "2_1", "2_2" )
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
			@DocumentId
			Integer id;
			@IndexedEmbedded(prefix = "level1_")
			List<IndexedEmbeddedLevel1> level1 = new ArrayList<>();
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "level1_level1Property", String.class, b2 -> b2.multiValued( true ) )
				.field( "level1_level2NoDotInPrefix_level2Property", String.class, b2 -> b2.multiValued( true ) )
				.objectField( "level1_level2OneDotInPrefix", ObjectStructure.DEFAULT, b2 -> b2
						.multiValued( true )
						// Not a direct child of level1: should be single-valued
						.field( "level2Property", String.class )
				)
				.objectField( "level1_level2TwoDotsInPrefix", ObjectStructure.DEFAULT, b2 -> b2
						.multiValued( true )
						// Not a direct child of level1: should be single-valued
						.objectField( "level3", ObjectStructure.DEFAULT, b3 -> b3
								// Not a direct child of level1: should be single-valued
								.field( "level2Property", String.class )
						)
				)
		);
		SearchMapping mapping = setupHelper.start()
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
			@GenericField
			String level1Property;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(prefix = "customPrefix_")
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Property) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Property;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "customPrefix_level1Property", String.class )
		);
		SearchMapping mapping = setupHelper.start()
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
			@DocumentId
			Integer id;
			@IndexedEmbedded(includePaths = "includedProperty")
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String ignoredProperty, String includedProperty) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.ignoredProperty = ignoredProperty;
				this.level1.includedProperty = includedProperty;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "includedProperty", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
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
			@GenericField
			String ignoredProperty;
			@GenericField
			String includedProperty;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(includePaths = {"includedProperty", "nonMatchingPath"})
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String ignoredProperty, String includedProperty) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.ignoredProperty = ignoredProperty;
				this.level1.includedProperty = includedProperty;
			}
		}

		Assertions.assertThatThrownBy(
				() -> setupHelper.start()
						.withAnnotatedEntityTypes( IndexedEntity.class )
						.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
						.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
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
	 * Check that the "includeDepth" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void includeDepth() {
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
			@DocumentId
			Integer id;
			@IndexedEmbedded(includeDepth = 1)
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value, String level2Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
				this.level1.level2 = new IndexedEmbeddedLevel2();
				this.level1.level2.level2Property = level2Value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
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
	 * Check that the "maxDepth" parameter is at least taken into account.
	 */
	@Test
	@SuppressWarnings("deprecation")
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
			@DocumentId
			Integer id;
			@IndexedEmbedded(maxDepth = 1)
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value, String level2Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
				this.level1.level2 = new IndexedEmbeddedLevel2();
				this.level1.level2.level2Property = level2Value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
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
	 * Check that the "structure" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void structure() {
		class IndexedEmbeddedLevel1 {
			@GenericField
			String level1Property;
			public String getLevel1Property() {
				return level1Property;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(structure = ObjectStructure.NESTED)
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", ObjectStructure.NESTED, b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
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
	 * Check that the deprecated "storage" parameter is taken into account.
	 */
	@Test
	@SuppressWarnings("deprecation")
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
			@DocumentId
			Integer id;
			@IndexedEmbedded(storage = org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage.NESTED)
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1();
				this.level1.level1Property = level1Value;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", ObjectStructure.NESTED, b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
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
		}

		StartupStubBridge.CounterKeys filteredOutBridgeCounterKeys = StartupStubBridge.createKeys();

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1IncludedField", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					b.addEntityType( IndexedEntity.class );
					TypeMappingStep indexedEntityMapping = b.programmaticMapping().type( IndexedEntity.class );
					indexedEntityMapping.indexed().index( INDEX_NAME );
					indexedEntityMapping.property( "id" ).documentId();
					indexedEntityMapping.property( "level1" )
							.indexedEmbedded()
									.includePaths( "level1IncludedField" );
					TypeMappingStep indexedEmbeddedLevel1Mapping = b.programmaticMapping().type( IndexedEmbeddedLevel1.class );
					indexedEmbeddedLevel1Mapping.binder( StartupStubBridge.binder( filteredOutBridgeCounterKeys ) );
					indexedEmbeddedLevel1Mapping.binder( new GeoPointBridge.Binder().fieldName( "location" ) );
					indexedEmbeddedLevel1Mapping.property( "latitude" ).marker( new LatitudeMarker.Binder() );
					indexedEmbeddedLevel1Mapping.property( "longitude" ).marker( new LongitudeMarker.Binder() );
					indexedEmbeddedLevel1Mapping.property( "level1Property" )
							.binder( StartupStubBridge.binder( filteredOutBridgeCounterKeys ) )
							.genericField( "level1IncludedField" )
							.genericField( "filteredOut" )
									.valueBinder( StartupStubBridge.binder( String.class, filteredOutBridgeCounterKeys ) );
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3072")
	public void targetType() {
		abstract class IndexedEmbeddedLevel1 {
			public abstract String getLevel1Property();
			public abstract void setLevel1Property(String level1Property);
		}
		class IndexedEmbeddedLevel1Impl extends IndexedEmbeddedLevel1 {
			String level1Property;
			@Override
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
			@Override
			public void setLevel1Property(String level1Property) {
				this.level1Property = level1Property;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(includeDepth = 1, targetType = IndexedEmbeddedLevel1Impl.class)
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value) {
				this.id = id;
				this.level1 = new IndexedEmbeddedLevel1Impl();
				this.level1.setLevel1Property( level1Value );
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3072")
	public void targetType_castException() {
		abstract class IndexedEmbeddedLevel1 {
			public abstract String getLevel1Property();
			public abstract void setLevel1Property(String level1Property);
		}
		class IndexedEmbeddedLevel1Impl extends IndexedEmbeddedLevel1 {
			String level1Property;
			@Override
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
			@Override
			public void setLevel1Property(String level1Property) {
				this.level1Property = level1Property;
			}
		}
		class InvalidTypeImpl extends IndexedEmbeddedLevel1 {
			String level1Property;
			@Override
			@GenericField
			public String getLevel1Property() {
				return level1Property;
			}
			@Override
			public void setLevel1Property(String level1Property) {
				this.level1Property = level1Property;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded(includeDepth = 1, targetType = IndexedEmbeddedLevel1Impl.class)
			IndexedEmbeddedLevel1 level1;
			public IndexedEntity(int id, String level1Value) {
				this.id = id;
				// The actual instance has a type that cannot be cast to IndexedEmbeddedLevel1Impl
				this.level1 = new InvalidTypeImpl();
				this.level1.setLevel1Property( level1Value );
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "level1", b2 -> b2
						.field( "level1Property", String.class )
				)
		);
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.withAnnotatedTypes( IndexedEmbeddedLevel1.class )
				.setup();
		backendMock.verifyExpectationsMet();

		assertThatThrownBy( () -> {
			try ( SearchSession session = mapping.createSession() ) {
				IndexedEntity entity1 = new IndexedEntity( 1, "level1Value" );

				session.indexingPlan().add( entity1 );
			}
		} )
				.isInstanceOf( ClassCastException.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-899")
	public void invalid_wrongType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			String text;
			@IndexedEmbedded
			String invalid;
		}

		Assertions.assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".invalid" )
						.failure( "Type '" + String.class.getName() +
								"' cannot be indexed-embedded, because no index mapping (@GenericField, @FullTextField, ...) is defined for that type." )
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-899")
	public void invalid_emptyNested() {
		class ValidNested {
			String text;

			@GenericField
			public String getText() {
				return text;
			}
		}

		class EmptyNested {
		}

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded
			ValidNested valid;
			@IndexedEmbedded
			EmptyNested invalid;
		}

		Assertions.assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedEntityTypes( IndexedEntity.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".invalid" )
						.failure( "Type '" + EmptyNested.class.getName() +
								"' cannot be indexed-embedded, because no index mapping (@GenericField, @FullTextField, ...) is defined for that type." )
						.build()
				);
	}

	private <E> void doTestEmbeddedRuntime(SearchMapping mapping,
			Function<Integer, E> newEntityFunction,
			Consumer<StubDocumentNode.Builder> expectedDocumentContributor) {
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = newEntityFunction.apply( 1 );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", expectedDocumentContributor )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}
}
