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
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuildContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

public class AnnotationMappingDiscoveryIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	@Test
	public void discoveryEnabled() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "annotationMappedEmbedded", b2 -> b2
						/*
						 * This field will only be added if the bridge is applied, which means:
						 * a) that the annotation mapping for the embedded type has been automatically discovered
						 * b) that the annotation mapping for the type on which the bridge is applied
						 * has been automatically discovered
						 */
						.objectField( "annotatedProperty", b3 -> {
							// We do not expect any particular property in the object field added by the bridge
						} )
				)
				.objectField( "nonAnnotationMappedEmbedded", b2 -> b2
						/*
						 * This field will be discovered automatically even though it is declared in an annotated type
						 * which has not been registered explicitly.
						 */
						.field( "text", String.class )
				)
		);

		setupHelper.start()
				.withConfiguration( builder -> {
					builder.addEntityType( IndexedEntity.class );

					// Do not register NonExplicitlyRegistered* types, they should be discovered automatically if required
					builder.annotationMapping().add( IndexedEntity.class );

					builder.programmaticMapping()
							.type( IndexedEntity.class )
									.property( "nonAnnotationMappedEmbedded" )
											.indexedEmbedded();
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void discoveryDisabled() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "annotationMappedEmbedded", b2 -> {
					/*
					 * This object field should be empty because
					 * the annotation mapping for the embedded type has *NOT* been automatically discovered.
					 */
				} )
				.objectField( "nonAnnotationMappedEmbedded", b2 -> {
					/*
					 * This object field should be empty because
					 * the annotation mapping for the embedded type has *NOT* been automatically discovered.
					 */
				} )
		);

		setupHelper.start()
				.withConfiguration( builder -> {
					builder.setAnnotatedTypeDiscoveryEnabled( false );
					builder.addEntityType( IndexedEntity.class );

					// Do not register NonExplicitlyRegistered* types, they should be discovered automatically if required
					builder.annotationMapping().add( IndexedEntity.class );

					builder.programmaticMapping()
							.type( IndexedEntity.class )
									.property( "nonAnnotationMappedEmbedded" )
											.indexedEmbedded();
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {
		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private NonExplicitlyRegisteredType annotationMappedEmbedded;

		private NonExplicitlyRegisteredNonAnnotationMappedType nonAnnotationMappedEmbedded;

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@IndexedEmbedded
		public NonExplicitlyRegisteredType getAnnotationMappedEmbedded() {
			return annotationMappedEmbedded;
		}

		public void setAnnotationMappedEmbedded(NonExplicitlyRegisteredType annotationMappedEmbedded) {
			this.annotationMappedEmbedded = annotationMappedEmbedded;
		}

		public NonExplicitlyRegisteredNonAnnotationMappedType getNonAnnotationMappedEmbedded() {
			return nonAnnotationMappedEmbedded;
		}

		public void setNonAnnotationMappedEmbedded(
				NonExplicitlyRegisteredNonAnnotationMappedType nonAnnotationMappedEmbedded) {
			this.nonAnnotationMappedEmbedded = nonAnnotationMappedEmbedded;
		}
	}

	/**
	 * A type that is not registered explicitly, but mentioned in an indexed-embedded property.
	 * It should be automatically discovered when applying the indexed-embedded,
	 * BUT the fact that it is indexed should be ignored (only explicitly registered types are indexed).
	 */
	@Indexed(index = "SHOULD_NOT_BE_INDEXED")
	public static class NonExplicitlyRegisteredType {
		private NonExplicitlyRegisteredNonMappedType content;

		@CustomMarkerConsumingPropertyBridgeAnnotation
		public NonExplicitlyRegisteredNonMappedType getContent() {
			return content;
		}

		public void setContent(NonExplicitlyRegisteredNonMappedType content) {
			this.content = content;
		}
	}

	/**
	 * A type that is neither registered explicitly, nor mentioned in any mapped property,
	 * but should be automatically discovered when the {@link CustomMarkerConsumingPropertyBridge} inspects the metamodel;
	 * if it isn't, the bridge will not contribute any field.
	 */
	public static class NonExplicitlyRegisteredNonMappedType {
		private Integer annotatedProperty;

		@CustomMarkerAnnotation
		public Integer getAnnotatedProperty() {
			return annotatedProperty;
		}

		public void setAnnotatedProperty(Integer annotatedProperty) {
			this.annotatedProperty = annotatedProperty;
		}
	}

	/**
	 * A type that is neither registered explicitly, nor mentioned in any annotation-mapped property,
	 * nor used by any bridge, but is mentioned in an programmatically mapped property.
	 * It should be automatically discovered when contributing the programmatic mapping;
	 * if it isn't, the field "nonAnnotationMappedEmbedded.text" will be missing.
	 */
	@Indexed(index = "SHOULD_NOT_BE_INDEXED")
	public static class NonExplicitlyRegisteredNonAnnotationMappedType {
		private String text;

		@GenericField
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.FIELD})
	@MarkerMapping(marker = @MarkerRef(builderType = CustomMarker.Builder.class))
	private @interface CustomMarkerAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.FIELD})
	@PropertyBridgeMapping(bridge = @PropertyBridgeRef(type = CustomMarkerConsumingPropertyBridge.class))
	private @interface CustomMarkerConsumingPropertyBridgeAnnotation {

	}

	private static final class CustomMarker {
		public static class Builder implements AnnotationMarkerBuilder<CustomMarkerAnnotation> {
			@Override
			public void initialize(CustomMarkerAnnotation annotation) {
				// Nothing to do
			}

			@Override
			public Object build(MarkerBuildContext buildContext) {
				return new CustomMarker();
			}
		}

		private CustomMarker() {
		}
	}

	public static final class CustomMarkerConsumingPropertyBridge implements PropertyBridge {
		private List<IndexObjectFieldReference> objectFieldReferences = new ArrayList<>();

		@Override
		public void bind(PropertyBridgeBindingContext context) {
			List<PojoModelProperty> markedProperties = context.getBridgedElement().properties()
					.filter( property -> property.markers( CustomMarker.class ).findAny().isPresent() )
					.collect( Collectors.toList() );
			for ( PojoModelProperty property : markedProperties ) {
				property.createAccessor();
				objectFieldReferences.add(
						context.getIndexSchemaElement().objectField( property.getName() ).toReference()
				);
			}
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			for ( IndexObjectFieldReference reference : objectFieldReferences ) {
				target.addObject( reference );
			}
		}
	}
}
