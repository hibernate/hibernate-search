/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.mapper.pojo.mapping.annotation.processing.CustomPropertyMappingAnnotationBaseIT;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) property bridges.
 * <p>
 * Does not test reindexing in depth; this is tested in {@code AutomaticIndexing*} tests in the ORM mapper.
 * <p>
 * Does not test custom annotations; this is tested in {@link CustomPropertyMappingAnnotationBaseIT}.
 */
@SuppressWarnings("unused")
public class PropertyBridgeBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );
	/**
	 * Basic test checking that a "normal" custom property bridge will work as expected
	 * when relying on accessors.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-2055", "HSEARCH-2641"})
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

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.property( "stringProperty" )
						.binder( context -> {
							PojoElementAccessor<String> pojoPropertyAccessor = context.bridgedElement()
									.createAccessor( String.class );
							IndexFieldReference<String> indexFieldReference = context.indexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
								target.addValue(
									indexFieldReference, pojoPropertyAccessor.read( bridgedElement )
								);
							} );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		entity.stringProperty = "some string";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", entity.stringProperty ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty = "some string 2";
			session.indexingPlan().addOrUpdate( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", entity.stringProperty ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Basic test checking that a "normal" custom property bridge will work as expected
	 * when relying on explicit dependency declaration.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitDependencies() {
		class Contained {
			String stringProperty;
			public String getStringProperty() {
				return stringProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.property( "contained" )
						.binder( context -> {
							context.dependencies().use( "stringProperty" );
							IndexFieldReference<String> indexFieldReference = context.indexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
								Contained castedBridgedElement = (Contained) bridgedElement;
								target.addValue(
									indexFieldReference, castedBridgedElement.getStringProperty()
								);
							} );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		Contained contained = new Contained();
		entity.contained = contained;
		contained.stringProperty = "some string";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", contained.stringProperty ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			contained.stringProperty = "some string 2";
			session.indexingPlan().addOrUpdate( entity, new String[] { "contained.stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", contained.stringProperty ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitDependencies_error_invalidProperty() {
		class Contained {
			String stringProperty;
			public String getStringProperty() {
				return stringProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( context -> {
									context.dependencies().use( "doesNotExist.stringProperty" );
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.withAnnotatedTypes( Contained.class )
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure(
								"Unable to find property 'doesNotExist' on type '" + Contained.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitDependencies_error_invalidContainerExtractorPath() {
		class Contained {
			String stringProperty;
			public String getStringProperty() {
				return stringProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( context -> {
									context.dependencies()
											.use(
													ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
													"stringProperty"
											);
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure(
								"Cannot apply the requested container value extractor '"
								+ BuiltinContainerExtractors.COLLECTION
								+ "' (implementation class: '" + CollectionElementExtractor.class.getName()
								+ "') to type '" + Contained.class.getName() + "'"
						)
						.build()
				);
	}

	/**
	 * Basic test checking that a "normal" custom property bridge will work as expected
	 * when relying on explicit reindexing declaration.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing() {
		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
						.property( "child" )
						.binder( context -> {
							context.dependencies()
									.fromOtherEntity( PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class, "parent" )
									.use( "stringProperty" );
							IndexFieldReference<String> indexFieldReference = context.indexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity castedBridgedElement =
									(PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity) bridgedElement;
								/*
								* In a real application this would run a query,
								* but we don't have the necessary infrastructure here
								* so we'll cut short and just index a constant.
								* We just need to know the bridge is executed anyway.
								*/
								target.addValue( indexFieldReference, "constant" );
							} );
						} )
		)
				.setup(
						PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
						PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
						PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
				);
		backendMock.verifyExpectationsMet();

		PropertyBridgeExplicitIndexingClasses.IndexedEntity entity =
				new PropertyBridgeExplicitIndexingClasses.IndexedEntity();
		entity.id = 1;
		PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity containedLevel1Entity =
				new PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity();
		containedLevel1Entity.parent = entity;
		entity.child = containedLevel1Entity;
		PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity containedLevel2Entity =
				new PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity();
		containedLevel2Entity.parent = containedLevel1Entity;
		containedLevel2Entity.stringProperty = "some string";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );
			session.indexingPlan().add( containedLevel2Entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "constant" ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			containedLevel2Entity.stringProperty = "some string";
			session.indexingPlan().addOrUpdate( containedLevel2Entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", "constant" ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	private static class PropertyBridgeExplicitIndexingClasses {
		@Indexed(index = INDEX_NAME)
		static class IndexedEntity {
			Integer id;
			ContainedLevel1Entity child;
			NotEntity notEntity;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "parent")))
			public ContainedLevel1Entity getChild() {
				return child;
			}
			public NotEntity getNotEntity() {
				return notEntity;
			}
		}
		static class ContainedLevel1Entity {
			IndexedEntity parent;
			public IndexedEntity getParent() {
				return parent;
			}
		}
		static class ContainedLevel2Entity {
			ContainedLevel1Entity parent;
			DifferentEntity associationToDifferentEntity;
			String stringProperty;
			public ContainedLevel1Entity getParent() {
				return parent;
			}
			public String getStringProperty() {
				return stringProperty;
			}

			public DifferentEntity getAssociationToDifferentEntity() {
				return associationToDifferentEntity;
			}
		}
		static class DifferentEntity {
		}
		static class NotEntity {
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_use_invalidProperty() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( context -> {
									context.dependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"parent"
											)
											.use( "doesNotExist" );
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure(
								"Unable to find property 'doesNotExist' on type '"
								+ PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_invalidProperty() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( context -> {
									context.dependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"doesNotExist"
											);
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure(
								"Unable to find property 'doesNotExist' on type '" + PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_invalidContainerExtractorPath() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( context -> {
									context.dependencies()
											.fromOtherEntity(
													ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													PojoModelPath.parse( "parent" )
											);
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure(
								"Cannot apply the requested container value extractor '"
								+ BuiltinContainerExtractors.COLLECTION
								+ "' (implementation class: '" + CollectionElementExtractor.class.getName()
								+ "') to type '" + PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_bridgedElementNotEntityType() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "notEntity" )
								.binder( context -> {
									context.dependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"doesNotMatter"
											);
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.withAnnotatedTypes( PropertyBridgeExplicitIndexingClasses.NotEntity.class )
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".notEntity" )
						.failure(
								"'fromOtherEntity' can only be used when the bridged element has an entity type,"
								+ " but the bridged element has type '" + PropertyBridgeExplicitIndexingClasses.NotEntity.class.getName() + "',"
								+ " which is not an entity type."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_otherEntityTypeNotEntityType() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( context -> {
									context.dependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.NotEntity.class,
													"doesNotMatter"
											);
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.withAnnotatedTypes( PropertyBridgeExplicitIndexingClasses.NotEntity.class )
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure(
								"'fromOtherEntity' expects an entity type; "
								+ "type '" + PropertyBridgeExplicitIndexingClasses.NotEntity.class.getName()
								+ "' is not an entity type."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_inverseAssociationPathTargetsWrongType() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( context -> {
									context.dependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"associationToDifferentEntity"
											);
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
								PropertyBridgeExplicitIndexingClasses.DifferentEntity.class
						)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure(
								"The inverse association targets type '" + PropertyBridgeExplicitIndexingClasses.DifferentEntity.class.getName() + "',"
								+ " but a supertype or subtype of '" + PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class.getName() + "' was expected."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void missingDependencyDeclaration() {
		class Contained {
			String stringProperty;
			public String getStringProperty() {
				return stringProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( context -> {
									// Do not declare any dependency
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure(
								"The binder did not declare any dependency to the entity model during binding."
										+ " Declare dependencies using context.getDependencies().use(...) or,"
										+ " if the bridge really does not depend on the entity model, context.getDependencies().useRootOnly()"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void inconsistentDependencyDeclaration() {
		class Contained {
			String stringProperty;
			public String getStringProperty() {
				return stringProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		Assertions.assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( context -> {
									// Declare no dependency, but also a dependency: this is inconsistent.
									context.dependencies()
											.use( "stringProperty" )
											.useRootOnly();
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure(
								"The binder called context.getDependencies().useRootOnly() during binding,"
										+ " but also declared extra dependencies to the entity model."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	@SuppressWarnings("unchecked")
	public void useRootOnly() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			List<String> stringProperty = new ArrayList<>();
			@DocumentId
			public Integer getId() {
				return id;
			}
			public List<String> getStringProperty() {
				return stringProperty;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.property( "stringProperty" )
						.binder( context -> {
							context.dependencies().useRootOnly();
							IndexFieldReference<String> indexFieldReference = context.indexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
								List<String> castedBridgedElement = (List<String>) bridgedElement;
								for ( String string : castedBridgedElement ) {
									target.addValue( indexFieldReference, string );
								}
							} );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		entity.stringProperty.add( "value1" );

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "value1" ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty.add( "value2" );
			session.indexingPlan().addOrUpdate( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", "value1", "value2" ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that field definitions are forwarded to the backend.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3324")
	public void field() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "stringFromBridge", String.class )
				.field( "listFromBridge", Integer.class, b2 -> b2.multiValued( true ) )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class ).property( "contained" )
						.binder( context -> {
							context.dependencies().useRootOnly();
							// Single-valued field
							context.indexSchemaElement()
									.field( "stringFromBridge", f -> f.asString() )
									.toReference();
							// Multi-valued field
							context.indexSchemaElement()
									.field( "listFromBridge", f -> f.asInteger() )
									.multiValued()
									.toReference();
							context.bridge( new UnusedPropertyBridge() );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that object field definitions are forwarded to the backend.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3324")
	public void objectField() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "stringFromBridge", b2 -> b2
						.field( "value", String.class )
				)
				.objectField( "listFromBridge", ObjectStructure.NESTED, b2 -> b2
						.multiValued( true )
						.field( "value", Integer.class )
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class ).property( "contained" )
						.binder( context -> {
							context.dependencies().useRootOnly();
							// Single-valued field
							IndexSchemaObjectField stringObjectField = context.indexSchemaElement()
									.objectField( "stringFromBridge" );
							stringObjectField.toReference();
							stringObjectField.field( "value", f -> f.asString() )
									.toReference();
							// Multi-valued field
							IndexSchemaObjectField listObjectField = context.indexSchemaElement()
									.objectField( "listFromBridge", ObjectStructure.NESTED )
									.multiValued();
							listObjectField.toReference();
							listObjectField.field( "value", f -> f.asInteger() )
									.toReference();
							context.bridge( new UnusedPropertyBridge() );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that field template definitions are forwarded to the backend.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void fieldTemplate() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.fieldTemplate( "stringFromBridge", String.class, b2 -> b2
						.matchingPathGlob( "*_string" )
				)
				.fieldTemplate( "listFromBridge", Integer.class, b2 -> b2
						.multiValued( true )
						.matchingPathGlob( "*_list" )
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class ).property( "contained" )
						.binder( context -> {
							context.dependencies().useRootOnly();
							// Single-valued field
							context.indexSchemaElement()
									.fieldTemplate( "stringFromBridge", f -> f.asString() )
									.matchingPathGlob( "*_string" );
							// Multi-valued field
							context.indexSchemaElement()
									.fieldTemplate( "listFromBridge", f -> f.asInteger() )
									.matchingPathGlob( "*_list" )
									.multiValued();
							context.bridge( new UnusedPropertyBridge() );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that object field template definitions are forwarded to the backend.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3273")
	public void objectFieldTemplate() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Contained contained;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Contained getContained() {
				return contained;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectFieldTemplate( "stringFromBridge", b2 -> b2
						.matchingPathGlob( "*_string" )
				)
				.fieldTemplate( "stringFromBridge_value", String.class, b2 -> b2
						.matchingPathGlob( "*_string.value" )
				)
				.objectFieldTemplate( "listFromBridge", ObjectStructure.NESTED, b2 -> b2
						.multiValued( true )
						.matchingPathGlob( "*_list" )
				)
				.fieldTemplate( "listFromBridge_value", Integer.class, b2 -> b2
						.matchingPathGlob( "*_list.value" )
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class ).property( "contained" )
						.binder( context -> {
							context.dependencies().useRootOnly();
							// Single-valued field
							context.indexSchemaElement()
									.objectFieldTemplate( "stringFromBridge" )
									.matchingPathGlob( "*_string" );
							context.indexSchemaElement()
									.fieldTemplate( "stringFromBridge_value", f -> f.asString() )
									.matchingPathGlob( "*_string.value" );
							// Multi-valued field
							context.indexSchemaElement()
									.objectFieldTemplate( "listFromBridge", ObjectStructure.NESTED )
									.matchingPathGlob( "*_list" )
									.multiValued();
							context.indexSchemaElement()
									.fieldTemplate( "listFromBridge_value", f -> f.asInteger() )
									.matchingPathGlob( "*_list.value" );
							context.bridge( new UnusedPropertyBridge() );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void incompatibleRequestedType() {
		@Indexed
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
		Assertions.assertThatThrownBy(
				() -> setupHelper.start()
						.withConfiguration( b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "stringProperty" )
								.binder( context -> {
									context.bridgedElement().createAccessor( Integer.class );
									context.bridge( new UnusedPropertyBridge() );
								} )
						)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".stringProperty" )
						.failure(
								"Requested incompatible type for '.stringProperty<no value extractors>'",
								"'" + Integer.class.getName() + "'"
						)
						.build()
				);
	}

	private static class UnusedPropertyBridge implements PropertyBridge {
		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			throw new AssertionFailure( "Should not be called" );
		}
	}

}
