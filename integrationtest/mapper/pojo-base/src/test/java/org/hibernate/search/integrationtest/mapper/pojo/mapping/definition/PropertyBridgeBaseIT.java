/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test common use cases of (custom) property bridges.
 * <p>
 * Does not test reindexing in depth; this is tested in {@code AutomaticIndexing*} tests in the ORM mapper.
 * <p>
 * Does not test custom annotations; this is tested in {@code CustomPropertyMappingAnnotationBaseIT}.
 */
@SuppressWarnings("unused")
class PropertyBridgeBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a "normal" custom property bridge will work as expected
	 * when relying on accessors.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-2055", "HSEARCH-2641" })
	void accessors() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "someField", String.class, b2 -> {
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
							context.bridge(
									(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
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
					.add( "1", b -> b.field( "someField", entity.stringProperty ) );
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty = "some string 2";
			session.indexingPlan().addOrUpdate( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "someField", entity.stringProperty ) );
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
	void explicitDependencies() {
		class Contained {
			String stringProperty;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "someField", String.class, b2 -> {
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
							context.bridge( Contained.class,
									(DocumentElement target, Contained bridgedElement,
											PropertyBridgeWriteContext context1) -> {
										target.addValue( indexFieldReference, bridgedElement.stringProperty );
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
					.add( "1", b -> b.field( "someField", contained.stringProperty ) );
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			contained.stringProperty = "some string 2";
			session.indexingPlan().addOrUpdate( entity, new String[] { "contained.stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "someField", contained.stringProperty ) );
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Check that referencing an inaccessible property through "use" will work properly.
	 * <p>
	 * Inaccessible properties are properties that we can see through reflection,
	 * but that we cannot retrieve a value from at runtime,
	 * because the call to Field.setAccessible is denied by the JVM.
	 * For example: Enum#name (the field, not the method).
	 * <p>
	 * Before HSEARCH-4114 was fixed, this test used to fail with the following report:
	 *
	 * <pre>{@literal
	 *     Standalone POJO mapping:
	 *         type 'org.hibernate.search.integrationtest.mapper.pojo.mapping.definition.
	 *         PropertyBridgeBaseIT$PropertyBridgeExplicitDependenciesInaccessibleObjectClasses$IndexedEntity':
	 *             path '.myEnum':
	 *                 failures:
	 *                   - HSEARCH700079: Exception while retrieving property type model for 'name' on
	 *                   'org.hibernate.search.integrationtest.mapper.pojo.mapping.definition.
	 *                   PropertyBridgeBaseIT$PropertyBridgeExplicitDependenciesInaccessibleObjectClasses$MyEnum'.
	 * }</pre>
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitDependencies_inacessibleObject() {
		backendMock.expectSchema( INDEX_NAME, b -> b.field( "someField", String.class, b2 -> {
			b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
		} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping()
						.type( PropertyBridgeExplicitDependenciesInaccessibleObjectClasses.IndexedEntity.class )
						.property( "myEnum" )
						.binder( context -> {
							// This references the "name" field, which is not accessible
							context.dependencies().use( "name" );
							IndexFieldReference<String> indexFieldReference = context.indexSchemaElement()
									.field( "someField", f -> f.asString().analyzer( "myAnalyzer" ) )
									.toReference();
							context.bridge( PropertyBridgeExplicitDependenciesInaccessibleObjectClasses.MyEnum.class,
									(DocumentElement target,
											PropertyBridgeExplicitDependenciesInaccessibleObjectClasses.MyEnum bridgedElement,
											PropertyBridgeWriteContext context1) -> {
										target.addValue( indexFieldReference, bridgedElement.name() );
									} );
						} )
		)
				.setup( PropertyBridgeExplicitDependenciesInaccessibleObjectClasses.IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		// If the above didn't throw any exception, we're good.

		// Check that indexing works, just in case...
		PropertyBridgeExplicitDependenciesInaccessibleObjectClasses.IndexedEntity entity =
				new PropertyBridgeExplicitDependenciesInaccessibleObjectClasses.IndexedEntity();
		entity.id = 1;
		entity.myEnum = PropertyBridgeExplicitDependenciesInaccessibleObjectClasses.MyEnum.VALUE1;

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "VALUE1" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	static class PropertyBridgeExplicitDependenciesInaccessibleObjectClasses {
		@Indexed(index = INDEX_NAME)
		static class IndexedEntity {
			@DocumentId
			Integer id;
			MyEnum myEnum;
		}

		enum MyEnum {
			VALUE1, VALUE2
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitDependencies_error_invalidProperty() {
		class Contained {
			String stringProperty;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure( "No readable property named 'doesNotExist' on type '"
								+ Contained.class.getName() + "'" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitDependencies_error_invalidContainerExtractorPath() {
		class Contained {
			String stringProperty;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.binder( context -> {
									context.dependencies()
											.use(
													ContainerExtractorPath
															.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
													"stringProperty"
											);
									context.bridge( new UnusedPropertyBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure( "Invalid container extractor for type '" + Contained.class.getName() + "': '"
								+ BuiltinContainerExtractors.COLLECTION
								+ "' (implementation class: '" + CollectionElementExtractor.class.getName() + "')" ) );
	}

	/**
	 * Basic test checking that a "normal" custom property bridge will work as expected
	 * when relying on explicit reindexing declaration.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitReindexing() {
		backendMock.expectSchema( INDEX_NAME, b -> b.field( "someField", String.class, b2 -> {
			b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
		} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
						.property( "child" )
						.binder( context -> {
							context.dependencies()
									.fromOtherEntity( PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
											"parent" )
									.use( "stringProperty" );
							IndexFieldReference<String> indexFieldReference = context.indexSchemaElement().field(
									"someField",
									f -> f.asString().analyzer( "myAnalyzer" )
							)
									.toReference();
							context.bridge( PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class,
									(DocumentElement target,
											PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity bridgedElement,
											PropertyBridgeWriteContext context1) -> {
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
			session.indexingPlan().add( 1, null, containedLevel2Entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "constant" ) );
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			containedLevel2Entity.stringProperty = "some string";
			session.indexingPlan().addOrUpdate( 1, null, containedLevel2Entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "someField", "constant" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	private static class PropertyBridgeExplicitIndexingClasses {
		@Indexed(index = INDEX_NAME)
		static class IndexedEntity {
			@DocumentId
			Integer id;
			@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "parent")))
			ContainedLevel1Entity child;
			NotEntity notEntity;
		}

		static class ContainedLevel1Entity {
			IndexedEntity parent;
		}

		static class ContainedLevel2Entity {
			ContainedLevel1Entity parent;
			DifferentEntity associationToDifferentEntity;
			String stringProperty;
		}

		static class DifferentEntity {
		}

		static class NotEntity {
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitReindexing_error_use_invalidProperty() {
		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure( "No readable property named 'doesNotExist' on type '"
								+ PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class.getName() + "'" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitReindexing_error_fromOtherEntity_invalidProperty() {
		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure( "No readable property named 'doesNotExist' on type '"
								+ PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class.getName() + "'" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitReindexing_error_fromOtherEntity_invalidContainerExtractorPath() {
		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.binder( context -> {
									context.dependencies()
											.fromOtherEntity(
													ContainerExtractorPath
															.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure( "Invalid container extractor for type '"
								+ PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class.getName() + "': '"
								+ BuiltinContainerExtractors.COLLECTION
								+ "' (implementation class: '" + CollectionElementExtractor.class.getName() + "')" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitReindexing_error_fromOtherEntity_bridgedElementNotEntityType() {
		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".notEntity" )
						.failure(
								"Invalid use of 'fromOtherEntity': this method can only be used when the bridged element has an entity type,"
										+ " but the bridged element has type '"
										+ PropertyBridgeExplicitIndexingClasses.NotEntity.class.getName() + "',"
										+ " which is not an entity type."
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitReindexing_error_fromOtherEntity_otherEntityTypeNotEntityType() {
		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure(
								"Invalid type passed to 'fromOtherEntity': the type must be an entity type",
								"Type '" + PropertyBridgeExplicitIndexingClasses.NotEntity.class.getName()
										+ "' is not an entity type."
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void explicitReindexing_error_fromOtherEntity_inverseAssociationPathTargetsWrongType() {
		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class.getName() )
						.pathContext( ".child" )
						.failure(
								"The inverse association targets type '"
										+ PropertyBridgeExplicitIndexingClasses.DifferentEntity.class.getName() + "',"
										+ " but a supertype or subtype of '"
										+ PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class.getName()
										+ "' was expected."
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void missingDependencyDeclaration() {
		class Contained {
			String stringProperty;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure(
								"Incorrect binder implementation",
								"the binder did not declare any dependency to the entity model during binding."
										+ " Declare dependencies using context.dependencies().use(...) or,"
										+ " if the bridge really does not depend on the entity model, context.dependencies().useRootOnly()"
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	void inconsistentDependencyDeclaration() {
		class Contained {
			String stringProperty;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".contained" )
						.failure(
								"Incorrect binder implementation",
								"the binder called context.dependencies().useRootOnly() during binding,"
										+ " but also declared extra dependencies to the entity model."
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void useRootOnly() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			List<String> stringProperty = new ArrayList<>();
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "someField", String.class, b2 -> {
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
							context.bridge( List.class,
									(DocumentElement target, List bridgedElement,
											PropertyBridgeWriteContext context1) -> {
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
					.add( "1", b -> b.field( "someField", "value1" ) );
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.stringProperty.add( "value2" );
			session.indexingPlan().addOrUpdate( entity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "someField", "value1", "value2" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that field definitions are forwarded to the backend.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3324")
	void field() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
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
	void objectField() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "stringFromBridge", b2 -> b2
						.field( "value", String.class )
				)
				.objectField( "listFromBridge", b2 -> b2
						.objectStructure( ObjectStructure.NESTED )
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
	void fieldTemplate() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
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
	void objectFieldTemplate() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectFieldTemplate( "stringFromBridge", b2 -> b2
						.matchingPathGlob( "*_string" )
				)
				.fieldTemplate( "stringFromBridge_value", String.class, b2 -> b2
						.matchingPathGlob( "*_string.value" )
				)
				.objectFieldTemplate( "listFromBridge", b2 -> b2
						.objectStructure( ObjectStructure.NESTED )
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
	void accessors_incompatibleRequestedType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}
		assertThatThrownBy(
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
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".stringProperty" )
						.failure( "'.stringProperty<no value extractors>' cannot be assigned to '"
								+ Integer.class.getName() + "'" ) );
	}

	private static class UnusedPropertyBridge implements PropertyBridge<Object> {
		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			throw new AssertionFailure( "Should not be called" );
		}
	}

	@Test
	void propertyBridge_invalidInputType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			@GenericField
			@PropertyBinding(binder = @PropertyBinderRef(type = MyStringBridge.Binder.class))
			Integer id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure( "Invalid bridge for input type '" + Integer.class.getName()
								+ "': '" + MyStringBridge.TOSTRING + "'",
								"This bridge expects an input of type '" + String.class.getName() + "'" ) );
	}

	public static class MyStringBridge implements PropertyBridge<String> {
		private static final String TOSTRING = "<MyStringPropertyBridge toString() result>";

		@Override
		public void write(DocumentElement target, String bridgedElement, PropertyBridgeWriteContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		@Override
		public String toString() {
			return TOSTRING;
		}

		public static class Binder implements PropertyBinder {
			@Override
			public void bind(PropertyBindingContext context) {
				context.bridge( String.class, new MyStringBridge() );
			}
		}
	}

	/**
	 * Test for backward compatibility with 6.0.0.CR1 APIs
	 */
	@Test
	void propertyBridge_noGenericType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			@PropertyBinding(binder = @PropertyBinderRef(type = RawTypeBridge.Binder.class))
			Integer id;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "someField", String.class ) );
		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 739;

			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "739", b -> b.field( "someField", "739" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@SuppressWarnings("rawtypes")
	public static class RawTypeBridge implements PropertyBridge {

		private final IndexFieldReference<String> fieldReference;

		public RawTypeBridge(IndexFieldReference<String> fieldReference) {
			this.fieldReference = fieldReference;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			target.addValue( fieldReference, bridgedElement.toString() );
		}

		public static class Binder implements PropertyBinder {
			@Override
			@SuppressWarnings("unchecked")
			public void bind(PropertyBindingContext context) {
				context.dependencies().useRootOnly();

				IndexFieldReference<String> fieldReference = context.indexSchemaElement().field(
						"someField", f -> f.asString() ).toReference();
				context.bridge( new RawTypeBridge( fieldReference ) );
			}
		}
	}

	/**
	 * Test that named predicate definitions are forwarded to the backend.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4166")
	void namedPredicate() {
		class Contained {
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Contained contained;
		}

		PredicateDefinition predicateDefinition = context -> {
			throw new IllegalStateException( "should not be used" );
		};

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "string", String.class, b2 -> {} )
				.namedPredicate( "named", b2 -> b2
						.predicateDefinition( predicateDefinition )
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class ).property( "contained" )
						.binder( context -> {
							context.dependencies().useRootOnly();
							context.indexSchemaElement()
									.field( "string", f -> f.asString() )
									.toReference();
							context.indexSchemaElement()
									.namedPredicate( "named", predicateDefinition );
							context.bridge( new UnusedPropertyBridge() );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}
}
