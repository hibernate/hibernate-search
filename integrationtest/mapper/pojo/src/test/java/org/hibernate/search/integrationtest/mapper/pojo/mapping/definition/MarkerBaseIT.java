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

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuildContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of markers and their mapping.
 * <p>
 * Does not test markers in depth for now;
 * {@link org.hibernate.search.integrationtest.mapper.pojo.spatial.AnnotationMappingGeoPointBridgeIT}
 * and {@link org.hibernate.search.integrationtest.mapper.pojo.spatial.ProgrammaticMappingGeoPointBridgeIT}
 * should address that.
 */
@SuppressWarnings("unused")
public class MarkerBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void error_missingBuilderReference() {
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
	@MarkerMapping(marker = @MarkerRef)
	private @interface MarkerAnnotationWithEmptyMarkerMapping {
	}

	@Test
	public void error_invalidAnnotationType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@MarkerAnnotationMappedToMarkerBuilderWithDifferentAnnotationType
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
						.failure(
								"Builder '" + MarkerBuilderWithDifferentAnnotationType.TOSTRING
										+ "' cannot be initialized with annotations of type '"
										+ MarkerAnnotationMappedToMarkerBuilderWithDifferentAnnotationType.class.getName() + "'"
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@MarkerMapping(marker = @MarkerRef(builderType = MarkerBuilderWithDifferentAnnotationType.class))
	private @interface MarkerAnnotationMappedToMarkerBuilderWithDifferentAnnotationType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	private @interface DifferentAnnotationType {
	}

	public static class MarkerBuilderWithDifferentAnnotationType
			implements AnnotationMarkerBuilder<DifferentAnnotationType> {
		private static String TOSTRING = "<MarkerBuilderWithDifferentAnnotationType toString() result>";
		@Override
		public void initialize(DifferentAnnotationType annotation) {
			throw new UnsupportedOperationException( "This should not be called" );
		}
		@Override
		public Object build(MarkerBuildContext buildContext) {
			throw new UnsupportedOperationException( "This should not be called" );
		}
		@Override
		public String toString() {
			return TOSTRING;
		}
	}

}
