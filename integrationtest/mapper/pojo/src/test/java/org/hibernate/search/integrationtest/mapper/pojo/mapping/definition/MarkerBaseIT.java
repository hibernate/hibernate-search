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

import org.hibernate.search.integrationtest.mapper.pojo.spatial.AnnotationMappingGeoPointBindingIT;
import org.hibernate.search.integrationtest.mapper.pojo.spatial.ProgrammaticMappingGeoPointBindingIT;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
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
 * {@link AnnotationMappingGeoPointBindingIT}
 * and {@link ProgrammaticMappingGeoPointBindingIT}
 * should address that.
 */
@SuppressWarnings("unused")
public class MarkerBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void error_missingBuilderReference() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@BindingAnnotationWithEmptyMarkerBinderRef
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.annotationContextAnyParameters( BindingAnnotationWithEmptyMarkerBinderRef.class )
						.failure(
								"Annotation type '" + BindingAnnotationWithEmptyMarkerBinderRef.class.getName()
										+ "' is annotated with '" + MarkerBinding.class.getName() + "',"
										+ " but the binder reference is empty"
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@MarkerBinding(binder = @MarkerBinderRef)
	private @interface BindingAnnotationWithEmptyMarkerBinderRef {
	}

	@Test
	public void error_invalidAnnotationType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@BindingAnnotationWithBinderWithDifferentAnnotationType
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Binder '" + MarkerBinderWithDifferentAnnotationType.TOSTRING
										+ "' cannot be initialized with annotations of type '"
										+ BindingAnnotationWithBinderWithDifferentAnnotationType.class.getName() + "'"
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@MarkerBinding(binder = @MarkerBinderRef(type = MarkerBinderWithDifferentAnnotationType.class))
	private @interface BindingAnnotationWithBinderWithDifferentAnnotationType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	private @interface DifferentAnnotationType {
	}

	public static class MarkerBinderWithDifferentAnnotationType
			implements MarkerBinder<DifferentAnnotationType> {
		private static String TOSTRING = "<MarkerBinderWithDifferentAnnotationType toString() result>";

		@Override
		public void initialize(DifferentAnnotationType annotation) {
			throw new UnsupportedOperationException( "This should not be called" );
		}

		@Override
		public void bind(MarkerBindingContext context) {
			throw new UnsupportedOperationException( "This should not be called" );
		}

		@Override
		public String toString() {
			return TOSTRING;
		}
	}

}
