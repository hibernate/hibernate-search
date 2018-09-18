/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.AnnotationMappingSmokeIT;
import org.hibernate.search.integrationtest.mapper.pojo.smoke.ProgrammaticMappingSmokeIT;
import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@code @IndexedEmbedded} annotation.
 * <p>
 * Does not test all the corner cases of filtering when using {@link IndexedEmbedded#includePaths()} and
 * {@link IndexedEmbedded#maxDepth()}, which are tested in a unit test in the engine module
 * (the test is named {@code IndexSchemaNestingContextImplTest} at the time of this writing).
 * <p>
 * Does not test uses of container value extractors (for now). Some of them are tested in
 * {@link AnnotationMappingSmokeIT} and {@link ProgrammaticMappingSmokeIT}.
 */
public class IndexedEmbeddedBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void noParameter() {
		class IndexedEmbeddedLevel2 {
			String level2Property;
			@Field
			public String getLevel2Property() {
				return level2Property;
			}
		}
		class IndexedEmbeddedLevel1 {
			String level1Property;
			IndexedEmbeddedLevel2 level2;
			@Field
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
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock )
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
	 * Check that the "prefix" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void prefix() {
		class IndexedEmbeddedLevel1 {
			String level1Property;
			@Field
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
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock )
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
			@Field
			public String getIgnoredProperty() {
				return ignoredProperty;
			}
			@Field
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
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock )
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
	 * Check that the "maxDepth" parameter is at least taken into account.
	 * <p>
	 * Details of how filtering handles all corner cases is tested in the engine (see this class' javadoc).
	 */
	@Test
	public void maxDepth() {
		class IndexedEmbeddedLevel2 {
			String level2Property;
			@Field
			public String getLevel2Property() {
				return level2Property;
			}
		}
		class IndexedEmbeddedLevel1 {
			String level1Property;
			IndexedEmbeddedLevel2 level2;
			@Field
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
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock )
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
			@Field
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
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock )
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

	private <E> void doTestEmbeddedRuntime(JavaBeanMapping mapping,
			Function<Integer, E> newEntityFunction,
			Consumer<StubDocumentNode.Builder> expectedDocumentContributor) {
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			E entity1 = newEntityFunction.apply( 1 );

			manager.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", expectedDocumentContributor )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}
}
