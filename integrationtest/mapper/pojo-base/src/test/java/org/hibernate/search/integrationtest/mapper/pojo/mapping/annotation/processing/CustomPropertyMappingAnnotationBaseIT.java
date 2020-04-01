/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.annotation.processing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
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
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) property mapping annotations.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-3135")
public class CustomPropertyMappingAnnotationBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple property mapping will be applied as expected.
	 */
	@Test
	public void simple() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@WorkingAnnotation
			public String getText() {
				return text;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "myText", String.class )
		);

		SearchMapping mapping = setupHelper.start()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = WorkingAnnotation.Processor.class))
	private @interface WorkingAnnotation {
		class Processor implements PropertyMappingAnnotationProcessor<WorkingAnnotation> {
			@Override
			public void process(PropertyMappingStep mapping, WorkingAnnotation annotation,
					PropertyMappingAnnotationProcessorContext context) {
				mapping.genericField( "myText" );
			}
		}
	}

	@Test
	public void missingProcessorReference() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@AnnotationWithEmptyProcessorRef
			public Integer getId() {
				return id;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
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
	@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef())
	private @interface AnnotationWithEmptyProcessorRef {
	}

	@Test
	public void invalidAnnotationType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@AnnotationWithProcessorWithDifferentAnnotationType
			public Integer getId() {
				return id;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
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
		class Processor implements PropertyMappingAnnotationProcessor<CustomPropertyMappingAnnotationBaseIT.DifferentAnnotationType> {
			public static final String TO_STRING = "DifferentAnnotationType.Processor";

			@Override
			public void process(PropertyMappingStep mapping, CustomPropertyMappingAnnotationBaseIT.DifferentAnnotationType annotation,
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
