/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.mapper.pojo.mapping.annotation.processing.CustomTypeMappingAnnotationBaseIT;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) type bridges.
 * <p>
 * Does not test reindexing in depth; this is tested in {@code AutomaticIndexing*} tests in the ORM mapper.
 * <p>
 * Does not test custom annotations; this is tested in {@link CustomTypeMappingAnnotationBaseIT}.
 */
@SuppressWarnings("unused")
public class TypeBridgeBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a "normal" custom type bridge will work as expected
	 * when relying on accessors.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-2055", "HSEARCH-2641"})
	public void accessors() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.binder( (TypeBinder) context -> {
							PojoElementAccessor<String> pojoPropertyAccessor =
									context.bridgedElement().property( "stringProperty" )
											.createAccessor( String.class );
							IndexFieldReference<String> indexFieldReference =
									context.indexSchemaElement().field(
											"someField",
											f -> f.asString().analyzer( "myAnalyzer" )
									)
											.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context1) -> {
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
	 * Basic test checking that a "normal" custom type bridge will work as expected
	 * when relying on explicit dependency declaration.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitDependencies() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.binder( (TypeBinder) context -> {
							context.dependencies().use( "stringProperty" );
							IndexFieldReference<String> indexFieldReference =
									context.indexSchemaElement().field(
											"someField",
											f -> f.asString().analyzer( "myAnalyzer" )
									)
											.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context1) -> {
								IndexedEntity castedBridgedElement = (IndexedEntity) bridgedElement;
								target.addValue(
									indexFieldReference, castedBridgedElement.stringProperty
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitDependencies_error_invalidProperty() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									context.dependencies().use( "doesNotExist" );
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Unable to find a readable property 'doesNotExist' on type '" + IndexedEntity.class.getName() + "'"
						)
						.build()
				);
	}

	/**
	 * Basic test checking that a "normal" custom type bridge will work as expected
	 * when relying on explicit reindexing declaration.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.binder( (TypeBinder) context -> {
							context.dependencies()
									.fromOtherEntity( ContainedEntity.class, "parent" )
									.use( "stringProperty" );
							IndexFieldReference<String> indexFieldReference =
									context.indexSchemaElement().field(
											"someField",
											f -> f.asString().analyzer( "myAnalyzer" )
									)
											.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context1) -> {
								IndexedEntity castedBridgedElement = (IndexedEntity) bridgedElement;
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
				.setup( IndexedEntity.class, ContainedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		ContainedEntity containedEntity = new ContainedEntity();
		containedEntity.parent = entity;
		containedEntity.stringProperty = "some string";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );
			session.indexingPlan().add( containedEntity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "constant" ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			containedEntity.stringProperty = "some string";

			session.indexingPlan().addOrUpdate( containedEntity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", "constant" ) )
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_use_invalidProperty() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			public Integer getId() {
				return id;
			}
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									context.dependencies()
											.fromOtherEntity(
													ContainedEntity.class,
													"parent"
											)
											.use( "doesNotExist" );
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.setup( IndexedEntity.class, ContainedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Unable to find a readable property 'doesNotExist' on type '" + ContainedEntity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_invalidProperty() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									context.dependencies()
											.fromOtherEntity(
													ContainedEntity.class,
													"doesNotExist"
											);
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.setup( IndexedEntity.class, ContainedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Unable to find a readable property 'doesNotExist' on type '" + ContainedEntity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_bridgedElementNotEntityType() {
		class NotEntity {
			String stringProperty;
			public String getStringProperty() {
				return stringProperty;
			}
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@IndexedEmbedded
			NotEntity notEntity;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( NotEntity.class )
								.binder( (TypeBinder) context -> {
									context.dependencies()
											.fromOtherEntity(
													IndexedEntity.class,
													"doesNotMatter"
											);
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.withAnnotatedTypes( NotEntity.class )
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notEntity<no value extractors>" )
						.failure(
								"'fromOtherEntity' can only be used when the bridged element has an entity type,"
								+ " but the bridged element has type '" + NotEntity.class.getName() + "',"
								+ " which is not an entity type."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_otherEntityTypeNotEntityType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		class NotEntity {
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									context.dependencies()
											.fromOtherEntity(
													NotEntity.class,
													"doesNotMatter"
											);
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.withAnnotatedTypes( NotEntity.class )
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"'fromOtherEntity' expects an entity type; "
								+ "type '" + NotEntity.class.getName() + "' is not an entity type."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_inverseAssociationPathTargetsWrongType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		class DifferentEntity {
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
			DifferentEntity associationToDifferentEntity;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									context.dependencies()
											.fromOtherEntity(
													ContainedEntity.class,
													"associationToDifferentEntity"
											);
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.setup( IndexedEntity.class, ContainedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"The inverse association targets type '" + DifferentEntity.class.getName() + "',"
								+ " but a supertype or subtype of '" + IndexedEntity.class.getName() + "' was expected."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void missingDependencyDeclaration() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									// Do not declare any dependency
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"The binder did not declare any dependency to the entity model during binding."
								+ " Declare dependencies using context.dependencies().use(...) or,"
								+ " if the bridge really does not depend on the entity model, context.dependencies().useRootOnly()"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void inconsistentDependencyDeclaration() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}

		assertThatThrownBy(
				() -> setupHelper.start().withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									// Declare no dependency, but also a dependency: this is inconsistent.
									context.dependencies()
											.use( "stringProperty" )
											.useRootOnly();
									context.bridge( new UnusedTypeBridge() );
								} )
				)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"The binder called context.dependencies().useRootOnly() during binding,"
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
			@DocumentId
			Integer id;
			@IndexedEmbedded
			CustomEnum enumProperty;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "enumProperty", b2 -> b2
						.field( "someField", String.class )
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( CustomEnum.class )
						.binder( (TypeBinder) context -> {
							context.dependencies().useRootOnly();
							IndexFieldReference<String> indexFieldReference = context.indexSchemaElement().field(
									"someField",
									f -> f.asString()
							)
									.toReference();
							context.bridge( (DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context1) -> {
								CustomEnum castedBridgedElement = (CustomEnum) bridgedElement;
								// This is a strange way to use bridges,
								// but then again a type bridges that only uses the root *is* strange
								target.addValue( indexFieldReference, castedBridgedElement.stringProperty );
							} );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		entity.enumProperty = CustomEnum.VALUE1;

		try ( SearchSession session = mapping.createSession() ) {

			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.objectField( "enumProperty", b2 -> b2
									.field( "someField", entity.enumProperty.stringProperty )
							)
					)
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.enumProperty = CustomEnum.VALUE2;

			session.indexingPlan().addOrUpdate( entity, new String[] { "enumProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b
							.objectField( "enumProperty", b2 -> b2
									.field( "someField", entity.enumProperty.stringProperty )
							)
					)
					.processedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	private enum CustomEnum {
		VALUE1("value1String"),
		VALUE2("value2String");
		final String stringProperty;
		CustomEnum(String stringProperty) {
			this.stringProperty = stringProperty;
		}
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
			@DocumentId
			Integer id;
			@IndexedEmbedded
			Contained contained;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "contained", b2 -> b2
						.field( "stringFromBridge", String.class )
						.field( "listFromBridge", Integer.class, b3 -> b3.multiValued( true ) )
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( Contained.class )
						.binder( (TypeBinder) context -> {
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
							context.bridge( new UnusedTypeBridge() );
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
			@DocumentId
			Integer id;
			@IndexedEmbedded
			Contained contained;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "contained", b2 -> b2
						.objectField( "stringFromBridge", b3 -> b3
								.field( "value", String.class )
						)
						.objectField( "listFromBridge", ObjectStructure.NESTED, b3 -> b3
								.multiValued( true )
								.field( "value", Integer.class )
						)
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( Contained.class )
						.binder( (TypeBinder) context -> {
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
							context.bridge( new UnusedTypeBridge() );
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
			@DocumentId
			Integer id;
			@IndexedEmbedded
			Contained contained;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "contained", b2 -> b2
						.fieldTemplate( "stringFromBridge", String.class, b3 -> b3
								.matchingPathGlob( "*_string" )
						)
						.fieldTemplate( "listFromBridge", Integer.class, b3 -> b3
								.multiValued( true )
								.matchingPathGlob( "*_list" )
						)
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( Contained.class )
						.binder( (TypeBinder) context -> {
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
							context.bridge( new UnusedTypeBridge() );
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
			@DocumentId
			Integer id;
			@IndexedEmbedded
			Contained contained;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "contained", b2 -> b2
						.objectFieldTemplate( "stringFromBridge", b3 -> b3
								.matchingPathGlob( "*_string" )
						)
						.fieldTemplate( "stringFromBridge_value", String.class, b3 -> b3
								.matchingPathGlob( "*_string.value" )
						)
						.objectFieldTemplate( "listFromBridge", ObjectStructure.NESTED, b3 -> b3
								.multiValued( true )
								.matchingPathGlob( "*_list" )
						)
						.fieldTemplate( "listFromBridge_value", Integer.class, b3 -> b3
								.matchingPathGlob( "*_list.value" )
						)
				)
		);

		SearchMapping mapping = setupHelper.start().withConfiguration(
				b -> b.programmaticMapping().type( Contained.class )
						.binder( (TypeBinder) context -> {
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
							context.bridge( new UnusedTypeBridge() );
						} )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void accessors_incompatibleRequestedType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			String stringProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start()
						.withConfiguration( b -> b.programmaticMapping().type( IndexedEntity.class )
								.binder( (TypeBinder) context -> {
									context.bridgedElement().property( "stringProperty" )
											.createAccessor( Integer.class );
									context.bridge( new UnusedTypeBridge() );
								} )
						)
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Requested incompatible type for '.stringProperty<no value extractors>'",
								"'" + Integer.class.getName() + "'"
						)
						.build()
				);
	}

	private static class UnusedTypeBridge implements TypeBridge {
		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			throw new AssertionFailure( "Should not be called" );
		}
	}
}
