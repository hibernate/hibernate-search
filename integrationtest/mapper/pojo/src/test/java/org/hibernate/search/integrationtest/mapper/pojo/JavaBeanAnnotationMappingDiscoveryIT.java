/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMappingInitiator;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.integrationtest.mapper.pojo.bridge.CustomMarkerConsumingPropertyBridge;
import org.hibernate.search.integrationtest.mapper.pojo.bridge.annotation.CustomMarkerAnnotation;
import org.hibernate.search.integrationtest.mapper.pojo.bridge.annotation.CustomMarkerConsumingPropertyBridgeAnnotation;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.rule.StaticCounters;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class JavaBeanAnnotationMappingDiscoveryIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SearchMappingRepository mappingRepository;

	@Test
	public void discoveryEnabled() {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.setProperty( "index.default.backend", "stubBackend" );

		JavaBeanMappingInitiator initiator = JavaBeanMappingInitiator.create( mappingRepositoryBuilder );

		// Do not register NonExplicitlyRegistered* types, they should be discovered automatically if required
		initiator.annotationMapping().add( IndexedEntity.class );

		initiator.programmaticMapping()
				.type( IndexedEntity.class )
						.property( "nonAnnotationMappedEmbedded" )
								.indexedEmbedded();

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

		mappingRepository = mappingRepositoryBuilder.build();
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void discoveryDisabled() {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.setProperty( "index.default.backend", "stubBackend" );

		JavaBeanMappingInitiator initiator = JavaBeanMappingInitiator.create(
				mappingRepositoryBuilder, false
		);

		// Do not register NonExplicitlyRegistered* types, they should be discovered automatically if required
		initiator.annotationMapping().add( IndexedEntity.class );

		initiator.programmaticMapping()
				.type( IndexedEntity.class )
						.property( "nonAnnotationMappedEmbedded" )
								.indexedEmbedded();

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

		mappingRepository = mappingRepositoryBuilder.build();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( mappingRepository != null ) {
			mappingRepository.close();
		}
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

		@Field
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
