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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
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
			@AnnotationWithEmptyProcessorRef
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
						.annotationTypeContext( AnnotationWithEmptyProcessorRef.class )
						.failure(
								"The processor reference in meta-annotation '" + PropertyMapping.class.getName() + "'"
										+ " is empty."
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef)
	private @interface AnnotationWithEmptyProcessorRef {
	}

	@Test
	public void error_invalidAnnotationType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@AnnotationWithProcessorWithDifferentAnnotationType
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
						.annotationTypeContext( AnnotationWithProcessorWithDifferentAnnotationType.class )
						.failure(
								"Annotation processor '"
										+ DifferentAnnotationType.Processor.TO_STRING + "'"
										+ " expects annotations of incompatible type '"
										+ DifferentAnnotationType.class.getName() + "'."
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = DifferentAnnotationType.Processor.class))
	private @interface AnnotationWithProcessorWithDifferentAnnotationType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	private @interface DifferentAnnotationType {
		class Processor implements PropertyMappingAnnotationProcessor<DifferentAnnotationType> {
			public static final String TO_STRING = "DifferentAnnotationType.Processor";

			@Override
			public void process(PropertyMappingStep mapping, DifferentAnnotationType annotation,
					PropertyMappingAnnotationProcessorContext context) {
				throw new UnsupportedOperationException( "This should not be called" );
			}

			@Override
			public String toString() {
				return TO_STRING;
			}
		}
	}

}
