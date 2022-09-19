/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.mapping.definition;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.MarkerBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

public class AnnotationMappingDiscoveryIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

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

		ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							context.programmaticMapping()
									.type( IndexedEntity.class )
									.property( "nonAnnotationMappedEmbedded" )
									.indexedEmbedded();

							mapAlwaysPresentProperty( context.programmaticMapping() );
						}
				)
				.setup(
						IndexedEntity.class,
						NonExplicitlyRegisteredType.class,
						NonExplicitlyRegisteredNonMappedType.class,
						NonExplicitlyRegisteredNonAnnotationMappedType.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void discoveryDisabled() {
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

		ormSetupHelper.start()
				.withProperty( HibernateOrmMapperSettings.MAPPING_PROCESS_ANNOTATIONS, "false" )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							context.programmaticMapping()
									.type( IndexedEntity.class )
									.property( "nonAnnotationMappedEmbedded" )
									.indexedEmbedded();

							/*
							 * Annotations should be completely ignored.
							 * We add some of the annotation mapping programmatically,
							 * just to check that discovery is disabled for nested types.
							 */
							TypeMappingStep indexedEntityMapping = context.programmaticMapping().type( IndexedEntity.class );
							indexedEntityMapping.indexed().index( IndexedEntity.INDEX );
							indexedEntityMapping.property( "id" ).documentId();
							indexedEntityMapping.property( "annotationMappedEmbedded" )
									.indexedEmbedded();

							mapAlwaysPresentProperty( context.programmaticMapping() );
						}
				)
				// We register NonExplicitlyRegistered* types here, but it's only for Hibernate ORM.
				// None of those types will be passed to Hibernate Search, since annotation mapping is disabled.
				.setup(
						IndexedEntity.class,
						NonExplicitlyRegisteredType.class,
						NonExplicitlyRegisteredNonMappedType.class,
						NonExplicitlyRegisteredNonAnnotationMappedType.class
				);
		backendMock.verifyExpectationsMet();
	}

	private void mapAlwaysPresentProperty(ProgrammaticMappingConfigurationContext mapping) {
		mapping.type( NonExplicitlyRegisteredType.class )
				.property( "alwaysPresent" ).genericField();

		mapping.type( NonExplicitlyRegisteredNonAnnotationMappedType.class )
				.property( "alwaysPresent" ).genericField();
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {
		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Embedded
		@IndexedEmbedded
		private NonExplicitlyRegisteredType annotationMappedEmbedded;

		@Embedded
		private NonExplicitlyRegisteredNonAnnotationMappedType nonAnnotationMappedEmbedded;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

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
	 * It should be automatically discovered when applying the indexed-embedded.
	 */
	@Embeddable
	public static class NonExplicitlyRegisteredType extends AlwaysPresentPropertyType {
		@PropertyBinding(binder = @PropertyBinderRef(type = CustomMarkerConsumingPropertyBridge.Binder.class))
		private NonExplicitlyRegisteredNonMappedType content;

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
	@Embeddable
	public static class NonExplicitlyRegisteredNonMappedType {
		@MarkerBinding(binder = @MarkerBinderRef(type = CustomMarker.Binder.class))
		private Integer annotatedProperty;

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
	@Embeddable
	public static class NonExplicitlyRegisteredNonAnnotationMappedType extends AlwaysPresentPropertyType {
		@GenericField
		private String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@MappedSuperclass
	public static class AlwaysPresentPropertyType {
		private String alwaysPresent;

		public String getAlwaysPresent() {
			return alwaysPresent;
		}

		public void setAlwaysPresent(String alwaysPresent) {
			this.alwaysPresent = alwaysPresent;
		}
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
