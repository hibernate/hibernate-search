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

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
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
	 * Basic test checking that a "normal" custom type bridge will work as expected.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-2055", "HSEARCH-2641"})
	public void typeBridge() {
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
							private PojoModelElementAccessor<String> pojoPropertyAccessor;
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
							public void write(DocumentElement target, PojoElement source,
									TypeBridgeWriteContext context) {
								indexFieldReference.write( target, pojoPropertyAccessor.read( source ) );
							}
						} ) )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.stringProperty = "some string";
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", entity.stringProperty ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Basic test checking that a "normal" custom property bridge will work as expected.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-2055", "HSEARCH-2641"})
	public void propertyBridge() {
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
							private PojoModelElementAccessor<String> pojoPropertyAccessor;
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
							public void write(DocumentElement target, PojoElement source,
									PropertyBridgeWriteContext context) {
								indexFieldReference.write( target, pojoPropertyAccessor.read( source ) );
							}
						} ) )
		)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.stringProperty = "some string";
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", entity.stringProperty ) )
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
		public void write(DocumentElement target, PojoElement source, TypeBridgeWriteContext context) {
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
		public void write(DocumentElement target, PojoElement source, PropertyBridgeWriteContext context) {
			throw new UnsupportedOperationException( "This should not be called" );
		}
	}
}
