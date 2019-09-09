/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
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
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) property bridges and their annotation mapping.
 * <p>
 * Does not test reindexing in depth; this is tested in {@code AutomaticIndexing*} tests in the ORM mapper.
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
						.binder( (PropertyBinder<?>) context -> {
							PojoElementAccessor<String> pojoPropertyAccessor = context.getBridgedElement()
									.createAccessor( String.class );
							IndexFieldReference<String> indexFieldReference = context.getIndexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.setBridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
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
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", entity.stringProperty ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty = "some string 2";
			session.getMainWorkPlan().update( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", entity.stringProperty ) )
					.preparedThenExecuted();
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
						.binder( (PropertyBinder<?>) context -> {
							context.getDependencies().use( "stringProperty" );
							IndexFieldReference<String> indexFieldReference = context.getIndexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.setBridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
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
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", contained.stringProperty ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			contained.stringProperty = "some string 2";
			session.getMainWorkPlan().update( entity, new String[] { "contained.stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", contained.stringProperty ) )
					.preparedThenExecuted();
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

		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies().use( "doesNotExist.stringProperty" );
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.withAnnotatedTypes( Contained.class )
						.setup( IndexedEntity.class )
		)
				.assertThrown()
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

		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies()
											.use(
													ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
													"stringProperty"
											);
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.assertThrown()
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
						.binder( (PropertyBinder<?>) context -> {
							context.getDependencies()
									.fromOtherEntity( PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class, "parent" )
									.use( "stringProperty" );
							IndexFieldReference<String> indexFieldReference = context.getIndexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.setBridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
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
			session.getMainWorkPlan().add( entity );
			session.getMainWorkPlan().add( containedLevel2Entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "constant" ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			containedLevel2Entity.stringProperty = "some string";
			session.getMainWorkPlan().update( containedLevel2Entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", "constant" ) )
					.preparedThenExecuted();
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
		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"parent"
											)
											.use( "doesNotExist" );
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.assertThrown()
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
		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"doesNotExist"
											);
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.assertThrown()
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
		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies()
											.fromOtherEntity(
													ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													PojoModelPath.parse( "parent" )
											);
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.assertThrown()
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
		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "notEntity" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"doesNotMatter"
											);
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.withAnnotatedTypes( PropertyBridgeExplicitIndexingClasses.NotEntity.class )
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.assertThrown()
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
		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.NotEntity.class,
													"doesNotMatter"
											);
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.withAnnotatedTypes( PropertyBridgeExplicitIndexingClasses.NotEntity.class )
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class
						)
		)
				.assertThrown()
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
		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( (PropertyBinder<?>) context -> {
									context.getDependencies()
											.fromOtherEntity(
													PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
													"associationToDifferentEntity"
											);
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.setup(
								PropertyBridgeExplicitIndexingClasses.IndexedEntity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
								PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
								PropertyBridgeExplicitIndexingClasses.DifferentEntity.class
						)
		)
				.assertThrown()
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

		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( (PropertyBinder<?>) context -> {
									// Do not declare any dependency
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.assertThrown()
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

		SubTest.expectException(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( (PropertyBinder<?>) context -> {
									// Declare no dependency, but also a dependency: this is inconsistent.
									context.getDependencies()
											.use( "stringProperty" )
											.useRootOnly();
									context.setBridge( new UnusedPropertyBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.assertThrown()
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
						.binder( (PropertyBinder<?>) context -> {
							context.getDependencies().useRootOnly();
							IndexFieldReference<String> indexFieldReference = context.getIndexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.setBridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
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
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "value1" ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty.add( "value2" );
			session.getMainWorkPlan().update( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", "value1", "value2" ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3324")
	public void multiValuedField() {
		class Contained {
			String string;
			List<String> list;
			public String getString() {
				return string;
			}
			public List<String> getList() {
				return list;
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

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "stringFromBridge", String.class )
				.field( "listFromBridge", String.class, b2 -> b2.multiValued( true ) )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class ).property( "contained" )
						.binder( (PropertyBinder<?>) context -> {
							context.getDependencies().useRootOnly();
							// Single-valued field
							context.getIndexSchemaElement()
									.field( "stringFromBridge", f -> f.asString() )
									.toReference();
							// Multi-valued field
							context.getIndexSchemaElement()
									.field( "listFromBridge", f -> f.asString() )
									.multiValued()
									.toReference();
							context.setBridge( new UnusedPropertyBridge() );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}


	@Test
	public void mapping_error_missingBinderReference() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@BindingAnnotationWithEmptyPropertyBridgeRef
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.annotationContextAnyParameters( BindingAnnotationWithEmptyPropertyBridgeRef.class )
						.failure(
								"Annotation type '" + BindingAnnotationWithEmptyPropertyBridgeRef.class.getName()
										+ "' is annotated with '" + PropertyBinding.class.getName() + "',"
										+ " but the binder reference is empty."
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyBinding(binder = @PropertyBinderRef())
	private @interface BindingAnnotationWithEmptyPropertyBridgeRef {
	}

	@Test
	public void mapping_error_invalidAnnotationType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@BindingAnnotationWithBinderWithDifferentAnnotationType
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Binder '" + BinderWithDifferentAnnotationType.TOSTRING
										+ "' cannot be initialized with annotations of type '"
										+ BindingAnnotationWithBinderWithDifferentAnnotationType.class.getName() + "'"
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyBinding(binder = @PropertyBinderRef(type = BinderWithDifferentAnnotationType.class))
	private @interface BindingAnnotationWithBinderWithDifferentAnnotationType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	private @interface DifferentAnnotationType {
	}

	public static class BinderWithDifferentAnnotationType
			implements PropertyBinder<DifferentAnnotationType> {
		private static String TOSTRING = "<BinderWithDifferentAnnotationType toString() result>";
		@Override
		public void initialize(DifferentAnnotationType annotation) {
			throw new UnsupportedOperationException( "This should not be called" );
		}

		@Override
		public void bind(PropertyBindingContext context) {
			throw new UnsupportedOperationException( "This should not be called" );
		}

		@Override
		public String toString() {
			return TOSTRING;
		}
	}

	@Test
	public void mapping_error_incompatibleRequestedType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			String stringProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IncompatibleTypeRequestingBinding
			public String getStringProperty() {
				return stringProperty;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
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

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	@PropertyBinding(binder = @PropertyBinderRef(type = IncompatibleTypeRequestingPropertyBinder.class))
	private @interface IncompatibleTypeRequestingBinding {
	}

	public static class IncompatibleTypeRequestingPropertyBinder implements PropertyBinder<Annotation> {
		@Override
		public void bind(PropertyBindingContext context) {
			context.getBridgedElement().createAccessor( Integer.class );
			context.setBridge( new UnusedPropertyBridge() );
		}
	}

	private static class UnusedPropertyBridge implements PropertyBridge {
		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			throw new AssertionFailure( "Should not be called" );
		}
	}

}
