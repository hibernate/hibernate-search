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
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.declaration.MarkerMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.MarkerRef;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeRef;
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

@SuppressWarnings("unused")
public class BridgeIT {

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
	public void typeBridge_accessors() {
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
	public void typeBridge_explicitDependencies() {
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
	public void typeBridge_explicitDependencies_error_invalidProperty() {
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
	public void typeBridge_explicitReindexing() {
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
	public void typeBridge_explicitReindexing_error_use_invalidProperty() {
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
	public void typeBridge_explicitReindexing_error_fromOtherEntity_invalidProperty() {
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
	public void typeBridge_explicitReindexing_error_fromOtherEntity_bridgedElementNotEntityType() {
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
	public void typeBridge_explicitReindexing_error_fromOtherEntity_otherEntityTypeNotEntityType() {
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
	public void typeBridge_explicitReindexing_error_fromOtherEntity_inverseAssociationPathTargetsWrongType() {
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
	public void typeBridge_missingDependencyDeclaration() {
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
	public void typeBridge_inconsistentDependencyDeclaration() {
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
	public void typeBridge_useRootOnly() {
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

	/**
	 * Basic test checking that a "normal" custom property bridge will work as expected
	 * when relying on accessors.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-2055", "HSEARCH-2641"})
	public void propertyBridge_accessors() {
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
						.property( "stringProperty" ).bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
							private PojoElementAccessor<String> pojoPropertyAccessor;
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(PropertyBridgeBindingContext context) {
								pojoPropertyAccessor = context.getBridgedElement()
										.createAccessor( String.class );
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString().analyzer( "myAnalyzer" )
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									PropertyBridgeWriteContext context) {
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
	 * Basic test checking that a "normal" custom property bridge will work as expected
	 * when relying on explicit dependency declaration.
	 * <p>
	 * Note that reindexing is tested in depth in the ORM mapper integration tests.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void propertyBridge_explicitDependencies() {
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

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.property( "contained" ).bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(PropertyBridgeBindingContext context) {
								context.getDependencies().use( "stringProperty" );
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString().analyzer( "myAnalyzer" )
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									PropertyBridgeWriteContext context) {
								Contained castedBridgedElement = (Contained) bridgedElement;
								target.addValue( indexFieldReference, castedBridgedElement.getStringProperty() );
							}
						} ) )
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
	public void propertyBridge_explicitDependencies_error_invalidProperty() {
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
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies().use( "doesNotExist.stringProperty" );
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
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
	public void propertyBridge_explicitDependencies_error_invalidContainerExtractorPath() {
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
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies()
												.use(
														ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractor.COLLECTION ),
														"stringProperty"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
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
						.pathContext( ".contained" )
						.failure(
								"Cannot apply the requested container value extractor '"
										+ CollectionElementExtractor.class.getName()
										+ "' to type '" + Contained.class.getName() + "'"
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
	public void propertyBridge_explicitReindexing() {
		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class, b2 -> {
					b2.analyzerName( "myAnalyzer" ); // For HSEARCH-2641
				} )
		);

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
						.property( "child" )
						.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(PropertyBridgeBindingContext context) {
								context.getDependencies()
										.fromOtherEntity( PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class, "parent" )
										.use( "stringProperty" );
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString().analyzer( "myAnalyzer" )
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									PropertyBridgeWriteContext context) {
								PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity castedBridgedElement =
										(PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity) bridgedElement;
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
	public void propertyBridge_explicitReindexing_error_use_invalidProperty() {
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
														"parent"
												)
												.use( "doesNotExist" );
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
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
	public void propertyBridge_explicitReindexing_error_fromOtherEntity_invalidProperty() {
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
														"doesNotExist"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
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
	public void propertyBridge_explicitReindexing_error_fromOtherEntity_invalidContainerExtractorPath() {
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractor.COLLECTION ),
														PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
														PojoModelPath.parse( "parent" )
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
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
										+ CollectionElementExtractor.class.getName()
										+ "' to type '" + PropertyBridgeExplicitIndexingClasses.ContainedLevel1Entity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void propertyBridge_explicitReindexing_error_fromOtherEntity_bridgedElementNotEntityType() {
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "notEntity" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
														"doesNotMatter"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
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
	public void propertyBridge_explicitReindexing_error_fromOtherEntity_otherEntityTypeNotEntityType() {
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														PropertyBridgeExplicitIndexingClasses.NotEntity.class,
														"doesNotMatter"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
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
	public void propertyBridge_explicitReindexing_error_fromOtherEntity_inverseAssociationPathTargetsWrongType() {
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( PropertyBridgeExplicitIndexingClasses.IndexedEntity.class )
								.property( "child" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										context.getDependencies()
												.fromOtherEntity(
														PropertyBridgeExplicitIndexingClasses.ContainedLevel2Entity.class,
														"associationToDifferentEntity"
												);
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
										throw new AssertionFailure( "Should not be called" );
									}
								} ) )
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
	public void propertyBridge_missingDependencyDeclaration() {
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
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										// Do not declare any dependency
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
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
						.pathContext( ".contained" )
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
	public void propertyBridge_inconsistentDependencyDeclaration() {
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
				() -> setupHelper.withBackendMock( backendMock ).withConfiguration(
						b -> b.programmaticMapping().type( IndexedEntity.class )
								.property( "contained" )
								.bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
									@Override
									public void bind(PropertyBridgeBindingContext context) {
										// Declare no dependency, but also a dependency: this is inconsistent.
										context.getDependencies()
												.use( "stringProperty" )
												.useRootOnly();
									}

									@Override
									public void write(DocumentElement target, Object bridgedElement,
											PropertyBridgeWriteContext context) {
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
						.pathContext( ".contained" )
						.failure(
								"The bridge called context.getDependencies().useRootOnly() during binding,"
										+ " but also declared extra dependencies to the entity model."
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void propertyBridge_useRootOnly() {
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

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).withConfiguration(
				b -> b.programmaticMapping().type( IndexedEntity.class )
						.property( "stringProperty" ).bridge( (BridgeBuilder<PropertyBridge>) buildContext -> BeanHolder.of( new PropertyBridge() {
							private IndexFieldReference<String> indexFieldReference;

							@Override
							public void bind(PropertyBridgeBindingContext context) {
								context.getDependencies().useRootOnly();
								indexFieldReference = context.getIndexSchemaElement().field(
										"someField",
										f -> f.asString().analyzer( "myAnalyzer" )
								)
										.toReference();
							}

							@Override
							public void write(DocumentElement target, Object bridgedElement,
									PropertyBridgeWriteContext context) {
								List<String> castedBridgedElement = (List<String>) bridgedElement;
								for ( String string : castedBridgedElement ) {
									target.addValue( indexFieldReference, string );
								}
							}
						} ) )
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
	public void typeBridgeMapping_error_missingBridgeReference() {
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
	public void typeBridgeMapping_error_conflictingBridgeReferenceInBridgeMapping() {
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
	public void typeBridgeMapping_error_incompatibleRequestedType() {
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

	@Test
	public void propertyBridgeMapping_error_missingBridgeReference() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@BridgeAnnotationWithEmptyPropertyBridgeMapping
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
						.pathContext( ".id" )
						.annotationContextAnyParameters( BridgeAnnotationWithEmptyPropertyBridgeMapping.class )
						.failure(
								"Annotation type '" + BridgeAnnotationWithEmptyPropertyBridgeMapping.class.getName()
										+ "' is annotated with '" + PropertyBridgeMapping.class.getName() + "',"
										+ " but neither a bridge reference nor a bridge builder reference was provided."
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyBridgeMapping(bridge = @PropertyBridgeRef())
	private @interface BridgeAnnotationWithEmptyPropertyBridgeMapping {
	}

	@Test
	public void propertyBridgeMapping_error_conflictingBridgeReferenceInBridgeMapping() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@BridgeAnnotationWithConflictingReferencesInPropertyBridgeMapping
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
						.pathContext( ".id" )
						.annotationContextAnyParameters( BridgeAnnotationWithConflictingReferencesInPropertyBridgeMapping.class )
						.failure(
								"Annotation type '" + BridgeAnnotationWithConflictingReferencesInPropertyBridgeMapping.class.getName()
										+ "' is annotated with '" + PropertyBridgeMapping.class.getName() + "',"
										+ " but both a bridge reference and a bridge builder reference were provided"
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyBridgeMapping(bridge = @PropertyBridgeRef(name = "foo", builderName = "bar"))
	private @interface BridgeAnnotationWithConflictingReferencesInPropertyBridgeMapping {
	}

	@Test
	public void markerMapping_error_missingBuilderReference() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@MarkerAnnotationWithEmptyMarkerMapping
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
						.pathContext( ".id" )
						.annotationContextAnyParameters( MarkerAnnotationWithEmptyMarkerMapping.class )
						.failure(
								"Annotation type '" + MarkerAnnotationWithEmptyMarkerMapping.class.getName()
										+ "' is annotated with '" + MarkerMapping.class.getName() + "',"
										+ " but the marker builder reference is empty"
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@MarkerMapping(builder = @MarkerRef)
	private @interface MarkerAnnotationWithEmptyMarkerMapping {
	}

	@Test
	public void propertyBridgeMapping_error_incompatibleRequestedType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			String stringProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@IncompatibleTypeRequestingPropertyBridgeAnnotation
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
	@PropertyBridgeMapping(bridge = @PropertyBridgeRef(type = IncompatibleTypeRequestingPropertyBridge.class))
	private @interface IncompatibleTypeRequestingPropertyBridgeAnnotation {
	}

	public static class IncompatibleTypeRequestingPropertyBridge implements PropertyBridge {
		@Override
		public void bind(PropertyBridgeBindingContext context) {
			context.getBridgedElement().createAccessor( Integer.class );
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			throw new UnsupportedOperationException( "This should not be called" );
		}
	}

	@Test
	public void valueBridge_indexNullAs() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Integer integer;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(valueBridge = @ValueBridgeRef(type = ParsingValueBridge.class), indexNullAs = "7")
			public Integer getInteger() { return integer; }
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "integer", Integer.class, f -> f.indexNullAs( 7 ) )
		);

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					// Stub backend is not supposed to use 'indexNullAs' option
					.add( "1", b -> b.field( "integer", null ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void valueBridge_indexNullAs_noParsing() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Integer integer;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(valueBridge = @ValueBridgeRef(type = NoParsingValueBridge.class), indexNullAs = "7")
			public Integer getInteger() { return integer; }
		}

		SubTest.expectException( () -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "does not support parsing a value from a String" )
				.hasMessageContaining( "integer" );
	}

	public static class NoParsingValueBridge implements ValueBridge<Integer, Integer> {

		public NoParsingValueBridge() {
		}

		@Override
		public Integer toIndexedValue(Integer value, ValueBridgeToIndexedValueContext context) {
			return value;
		}

		@Override
		public Integer cast(Object value) {
			return (Integer) value;
		}
	}

	public static class ParsingValueBridge extends NoParsingValueBridge {

		public ParsingValueBridge() {
		}

		@Override
		public Integer parse(String value) {
			return Integer.parseInt( value );
		}
	}
}
