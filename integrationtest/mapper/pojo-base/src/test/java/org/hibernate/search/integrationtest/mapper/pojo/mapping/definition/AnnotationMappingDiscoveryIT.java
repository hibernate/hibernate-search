/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.MarkerBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AnnotationMappingDiscoveryIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void discoveryEnabled() {
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
						.field( "alwaysPresent", String.class )
				)
				.objectField( "nonAnnotationMappedEmbedded", b2 -> b2
						/*
						 * This field will be discovered automatically even though it is declared in an annotated type
						 * which has not been registered explicitly.
						 */
						.field( "text", String.class )
						.field( "alwaysPresent", String.class )
				)
		);

		setupHelper.start()
				.expectCustomBeans()
				.withConfiguration( builder -> {
					// Do not register NonExplicitlyRegistered* types, they should be discovered automatically if required
					builder.annotationMapping().add( IndexedEntity.class );

					builder.programmaticMapping()
							.type( IndexedEntity.class )
							.property( "nonAnnotationMappedEmbedded" )
							.indexedEmbedded();

					mapAlwaysPresentProperty( builder.programmaticMapping() );
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Test
	void discoveryDisabled() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "annotationMappedEmbedded", b2 -> {
					/*
					 * This object field should contain only the property mapped using the programmatic API,
					 * because the annotation mapping for the embedded type has *NOT* been automatically discovered.
					 */
					b2.field( "alwaysPresent", String.class );
				} )
				.objectField( "nonAnnotationMappedEmbedded", b2 -> {
					/*
					 * This object field should contain only the property mapped using the programmatic API,
					 * because the annotation mapping for the embedded type has *NOT* been automatically discovered.
					 */
					b2.field( "alwaysPresent", String.class );
				} )
		);

		setupHelper.start()
				.withConfiguration( builder -> {
					builder.annotationMapping().discoverAnnotationsFromReferencedTypes( false );

					// Do not register NonExplicitlyRegistered* types, they should be discovered automatically if required
					builder.annotationMapping().add( IndexedEntity.class );

					builder.programmaticMapping()
							.type( IndexedEntity.class )
							.property( "nonAnnotationMappedEmbedded" )
							.indexedEmbedded();

					mapAlwaysPresentProperty( builder.programmaticMapping() );
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	private void mapAlwaysPresentProperty(ProgrammaticMappingConfigurationContext mapping) {
		mapping.type( NonExplicitlyRegisteredType.class )
				.property( "alwaysPresent" ).genericField();

		mapping.type( NonExplicitlyRegisteredNonAnnotationMappedType.class )
				.property( "alwaysPresent" ).genericField();
	}

	@SearchEntity
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {
		public static final String INDEX = "IndexedEntity";

		@DocumentId
		private Integer id;

		@IndexedEmbedded
		private NonExplicitlyRegisteredType annotationMappedEmbedded;

		private NonExplicitlyRegisteredNonAnnotationMappedType nonAnnotationMappedEmbedded;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	/**
	 * A type that is not registered explicitly, but mentioned in an indexed-embedded property.
	 * It should be automatically discovered when applying the indexed-embedded.
	 */
	public static class NonExplicitlyRegisteredType extends AlwaysPresentPropertyType {
		@PropertyBinding(binder = @PropertyBinderRef(type = CustomMarkerConsumingPropertyBridge.Binder.class))
		private NonExplicitlyRegisteredNonMappedType content;
	}

	/**
	 * A type that is neither registered explicitly, nor mentioned in any mapped property,
	 * but should be automatically discovered when the {@link CustomMarkerConsumingPropertyBridge} inspects the metamodel;
	 * if it isn't, the bridge will not contribute any field.
	 */
	public static class NonExplicitlyRegisteredNonMappedType {
		@MarkerBinding(binder = @MarkerBinderRef(type = CustomMarker.Binder.class))
		private Integer annotatedProperty;
	}

	/**
	 * A type that is neither registered explicitly, nor mentioned in any annotation-mapped property,
	 * nor used by any bridge, but is mentioned in an programmatically mapped property.
	 * It should be automatically discovered when contributing the programmatic mapping;
	 * if it isn't, the field "nonAnnotationMappedEmbedded.text" will be missing.
	 */
	public static class NonExplicitlyRegisteredNonAnnotationMappedType extends AlwaysPresentPropertyType {
		@GenericField
		private String text;
	}

	public static class AlwaysPresentPropertyType {
		private String alwaysPresent;
	}

	private static final class CustomMarker {
		private CustomMarker() {
		}

		public static class Binder implements MarkerBinder {
			@Override
			public void bind(MarkerBindingContext context) {
				context.marker( new CustomMarker() );
			}
		}
	}

	public static final class CustomMarkerConsumingPropertyBridge implements PropertyBridge<Object> {
		private final List<IndexObjectFieldReference> objectFieldReferences = new ArrayList<>();

		private CustomMarkerConsumingPropertyBridge(PropertyBindingContext context) {
			for ( PojoModelProperty property : context.bridgedElement().properties() ) {
				if ( property.markers( CustomMarker.class ).isEmpty() ) {
					continue;
				}
				property.createAccessor();
				objectFieldReferences.add( context.indexSchemaElement().objectField( property.name() ).toReference() );
			}
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			for ( IndexObjectFieldReference reference : objectFieldReferences ) {
				target.addObject( reference );
			}
		}

		public static class Binder implements PropertyBinder {
			@Override
			public void bind(PropertyBindingContext context) {
				context.bridge( new CustomMarkerConsumingPropertyBridge( context ) );
			}
		}
	}
}
