/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) type bridges and their annotation mapping.
 * <p>
 * Does not test reindexing in depth; this is tested in {@code AutomaticIndexing*} tests in the ORM mapper.
 */
@SuppressWarnings("unused")
public class TypeBridgeBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

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

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
							private PojoElementAccessor<String> pojoPropertyAccessor;
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(TypeBridgeBindingContext context) {
								pojoPropertyAccessor = context.getBridgedElement()
										.property( "stringProperty" )
										.createAccessor( String.class );
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString().analyzer( "myAnalyzer" )
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									TypeBridgeWriteContext context) {
								target.addValue( indexFieldReference, pojoPropertyAccessor.read( bridgedElement ) );
							}
						} ) )
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

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(TypeBridgeBindingContext context) {
								context.getDependencies().use( "stringProperty" );
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString().analyzer( "myAnalyzer" )
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									TypeBridgeWriteContext context) {
								IndexedEntity castedBridgedElement = (IndexedEntity) bridgedElement;
								target.addValue( indexFieldReference, castedBridgedElement.getStringProperty() );
							}
						} ) )
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitDependencies_error_invalidProperty() {
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

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										context.getDependencies().use( "doesNotExist" );
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Unable to find property 'doesNotExist' on type '" + IndexedEntity.class.getName() + "'"
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
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
			public IndexedEntity getParent() {
				return parent;
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

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(TypeBridgeBindingContext context) {
								context.getDependencies()
										.fromOtherEntity( ContainedEntity.class, "parent" )
										.use( "stringProperty" );
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString().analyzer( "myAnalyzer" )
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									TypeBridgeWriteContext context) {
								IndexedEntity castedBridgedElement = (IndexedEntity) bridgedElement;
								/*
								 * In a real application this would run a query,
								 * but we don't have the necessary infrastructure here
								 * so we'll cut short and just index a constant.
								 * We just need to know the bridge is executed anyway.
								 */
								target.addValue( indexFieldReference, "constant" );
							}
						} ) )
		)
				.setup( IndexedEntity.class, ContainedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		ContainedEntity containedEntity = new ContainedEntity();
		containedEntity.parent = entity;
		containedEntity.stringProperty = "some string";

		try ( SearchSession session = mapping.createSession() ) {
			session.getMainWorkPlan().add( entity );
			session.getMainWorkPlan().add( containedEntity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "constant" ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			containedEntity.stringProperty = "some string";

			session.getMainWorkPlan().update( containedEntity, new String[] { "stringProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b.field( "someField", "constant" ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_use_invalidProperty() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
			public IndexedEntity getParent() {
				return parent;
			}
			public String getStringProperty() {
				return stringProperty;
			}
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														ContainedEntity.class,
														"parent"
												)
												.use( "doesNotExist" );
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.setup( IndexedEntity.class, ContainedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Unable to find property 'doesNotExist' on type '" + ContainedEntity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void explicitReindexing_error_fromOtherEntity_invalidProperty() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
			public IndexedEntity getParent() {
				return parent;
			}
			public String getStringProperty() {
				return stringProperty;
			}
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														ContainedEntity.class,
														"doesNotExist"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.setup( IndexedEntity.class, ContainedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"Unable to find property 'doesNotExist' on type '" + ContainedEntity.class.getName() + "'"
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
			Integer id;
			NotEntity notEntity;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded
			public NotEntity getNotEntity() {
				return notEntity;
			}
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( NotEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														IndexedEntity.class,
														"doesNotMatter"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.withAnnotatedTypes( NotEntity.class )
						.setup( IndexedEntity.class )
		)
				.assertThrown()
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
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		class NotEntity {
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														NotEntity.class,
														"doesNotMatter"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.withAnnotatedTypes( NotEntity.class )
						.setup( IndexedEntity.class )
		)
				.assertThrown()
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
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		class DifferentEntity {
		}
		class ContainedEntity {
			IndexedEntity parent;
			String stringProperty;
			DifferentEntity associationToDifferentEntity;
			public IndexedEntity getParent() {
				return parent;
			}
			public String getStringProperty() {
				return stringProperty;
			}
			public DifferentEntity getAssociationToDifferentEntity() {
				return associationToDifferentEntity;
			}
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														ContainedEntity.class,
														"associationToDifferentEntity"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.setup( IndexedEntity.class, ContainedEntity.class )
		)
				.assertThrown()
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

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										// Do not declare any dependency
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"The bridge did not declare any dependency to the entity model during binding."
								+ " Declare dependencies using context.getDependencies().use(...) or,"
								+ " if the bridge really does not depend on the entity model, context.getDependencies().useRootOnly()"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void inconsistentDependencyDeclaration() {
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

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
									@Override
									public void bind(TypeBridgeBindingContext context) {
										// Declare no dependency, but also a dependency: this is inconsistent.
										context.getDependencies()
												.use( "stringProperty" )
												.useRootOnly();
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											TypeBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
				)
						.setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.failure(
								"The bridge called context.getDependencies().useRootOnly() during binding,"
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
			CustomEnum enumProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IndexedEmbedded
			public CustomEnum getEnumProperty() {
				return enumProperty;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "enumProperty", b2 -> b2
						.field( "someField", String.class )
				)
		);

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( CustomEnum.class )
						.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(TypeBridgeBindingContext context) {
								context.getDependencies().useRootOnly();
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString()
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									TypeBridgeWriteContext context) {
								CustomEnum castedBridgedElement = (CustomEnum) bridgedElement;
								// This is a strange way to use bridges,
								// but then again a type bridges that only use the root *is* strange
								target.addValue( indexFieldReference, castedBridgedElement.stringProperty );
							}
						} ) )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		entity.enumProperty = CustomEnum.VALUE1;

		try ( SearchSession session = mapping.createSession() ) {

			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.objectField( "enumProperty", b2 -> b2
									.field( "someField", entity.enumProperty.stringProperty )
							)
					)
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			entity.enumProperty = CustomEnum.VALUE2;

			session.getMainWorkPlan().update( entity, new String[] { "enumProperty" } );

			backendMock.expectWorks( INDEX_NAME )
					.update( "1", b -> b
							.objectField( "enumProperty", b2 -> b2
									.field( "someField", entity.enumProperty.stringProperty )
							)
					)
					.preparedThenExecuted();
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
			@IndexedEmbedded
			public Contained getContained() {
				return contained;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.objectField( "contained", b2 -> b2
						.field( "stringFromBridge", String.class )
						.field( "listFromBridge", String.class, b3 -> b3.multiValued( true ) )
				)
		);

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( Contained.class )
						.bridge( (BridgeBuilder<TypeBridge>) buildContext -> BeanHolder.of( new TypeBridge() {
							@Override
							public void bind(TypeBridgeBindingContext context) {
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
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									TypeBridgeWriteContext context) {
								throw new UnsupportedOperationException( "This should not be called" );
							}
						} ) )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void mapping_error_missingBridgeReference() {
		@Indexed
		@BridgeAnnotationWithEmptyTypeBridgeRef
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.annotationContextAnyParameters( BridgeAnnotationWithEmptyTypeBridgeRef.class )
						.failure(
								"Annotation type '" + BridgeAnnotationWithEmptyTypeBridgeRef.class.getName()
										+ "' is annotated with '" + TypeBridgeMapping.class.getName() + "',"
										+ " but neither a bridge reference nor a bridge builder reference was provided."
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TypeBridgeMapping(bridge = @TypeBridgeRef)
	private @interface BridgeAnnotationWithEmptyTypeBridgeRef {
	}

	@Test
	public void mapping_error_conflictingBridgeReferenceInBridgeMapping() {
		@Indexed
		@BridgeAnnotationWithConflictingReferencesInTypeBridgeMapping
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.annotationContextAnyParameters( BridgeAnnotationWithConflictingReferencesInTypeBridgeMapping.class )
						.failure(
								"Annotation type '" + BridgeAnnotationWithConflictingReferencesInTypeBridgeMapping.class.getName()
										+ "' is annotated with '" + TypeBridgeMapping.class.getName() + "',"
										+ " but both a bridge reference and a bridge builder reference were provided"
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TypeBridgeMapping(bridge = @TypeBridgeRef(name = "foo", builderName = "bar"))
	private @interface BridgeAnnotationWithConflictingReferencesInTypeBridgeMapping {
	}

	@Test
	public void mapping_error_incompatibleRequestedType() {
		@Indexed
		@IncompatibleTypeRequestingTypeBridgeAnnotation
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
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
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

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TypeBridgeMapping(bridge = @TypeBridgeRef(type = IncompatibleTypeRequestingTypeBridge.class))
	private @interface IncompatibleTypeRequestingTypeBridgeAnnotation {
	}

	public static class IncompatibleTypeRequestingTypeBridge implements TypeBridge {
		@Override
		public void bind(TypeBridgeBindingContext context) {
			context.getBridgedElement().property( "stringProperty" ).createAccessor( Integer.class );
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			throw new UnsupportedOperationException( "This should not be called" );
		}
	}

}
