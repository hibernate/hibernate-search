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
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedType;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) type mapping annotations.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-3135")
public class CustomTypeMappingAnnotationBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple type mapping will be applied as expected.
	 */
	@Test
	public void simple() {
		@Indexed(index = INDEX_NAME)
		@WorkingAnnotation
		class IndexedEntity {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
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
	@Target({ElementType.TYPE})
	@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = WorkingAnnotation.Processor.class))
	private @interface WorkingAnnotation {
		class Processor implements TypeMappingAnnotationProcessor<WorkingAnnotation> {
			@Override
			public void process(
					TypeMappingStep mapping, WorkingAnnotation annotation,
					TypeMappingAnnotationProcessorContext context) {
				mapping.property( "text" ).genericField( "myText" );
			}
		}
	}

	@Test
	public void missingBinderReference() {
		@Indexed
		@AnnotationWithEmptyProcessorRef
		class IndexedEntity {
			Integer id;
			@DocumentId
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
								"The processor reference in meta-annotation '" + TypeMapping.class.getName() + "'"
										+ " is empty."
						)
						.build()
				);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TypeMapping(processor = @TypeMappingAnnotationProcessorRef)
	private @interface AnnotationWithEmptyProcessorRef {
	}

	@Test
	public void invalidAnnotationType() {
		@Indexed
		@AnnotationWithProcessorWithDifferentAnnotationType
		class IndexedEntity {
			Integer id;
			@DocumentId
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
	@Target(ElementType.TYPE)
	@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = DifferentAnnotationType.Processor.class))
	private @interface AnnotationWithProcessorWithDifferentAnnotationType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private @interface DifferentAnnotationType {
		class Processor implements TypeMappingAnnotationProcessor<CustomTypeMappingAnnotationBaseIT.DifferentAnnotationType> {
			public static final String TO_STRING = "DifferentAnnotationType.Processor";

			@Override
			public void process(TypeMappingStep mapping, CustomTypeMappingAnnotationBaseIT.DifferentAnnotationType annotation,
					TypeMappingAnnotationProcessorContext context) {
				throw new UnsupportedOperationException( "This should not be called" );
			}

			@Override
			public String toString() {
				return TO_STRING;
			}
		}
	}

	@Test
	public void annotatedElement() {
		final String index1Name = "index1";
		final String index2Name = "index2";
		final String index3Name = "index3";
		@Indexed(index = index1Name)
		@AnnotatedElementAwareAnnotation
		@AnalyzerAnnotation(name = "foo")
		class IndexedEntityType1 {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public String getText() {
				return text;
			}
		}
		@Indexed(index = index2Name)
		@AnnotatedElementAwareAnnotation
		class IndexedEntityType2 {
			Integer id;
			String keyword;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public String getKeyword() {
				return keyword;
			}
		}
		@Indexed(index = index3Name)
		@AnnotatedElementAwareAnnotation
		class IndexedEntityType3 {
			Integer id;
			Integer integer;
			@DocumentId
			public Integer getId() {
				return id;
			}
			public Integer getInteger() {
				return integer;
			}
		}

		backendMock.expectSchema( index1Name, b -> b
				.field( "myText", String.class, b2 -> b2.analyzerName( "foo" ) )
		);
		backendMock.expectSchema( index2Name, b -> b
				.field( "myKeyword", String.class )
		);
		backendMock.expectSchema( index3Name, b -> b
				.field( "myInteger", Integer.class )
		);

		SearchMapping mapping = setupHelper.start()
				.setup( IndexedEntityType1.class, IndexedEntityType2.class, IndexedEntityType3.class );
		backendMock.verifyExpectationsMet();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = AnnotatedElementAwareAnnotation.Processor.class))
	private @interface AnnotatedElementAwareAnnotation {
		class Processor implements TypeMappingAnnotationProcessor<AnnotatedElementAwareAnnotation> {
			@Override
			public void process(TypeMappingStep mapping, AnnotatedElementAwareAnnotation annotation,
					TypeMappingAnnotationProcessorContext context) {
				MappingAnnotatedType annotatedElement = context.annotatedElement();
				if ( annotatedElement.javaClass().getName().endsWith( "IndexedEntityType1" )
						|| annotatedElement.javaClass().getName().endsWith( "IndexedEntityType2" ) ) {
					Optional<String> analyzer = annotatedElement.allAnnotations()
							.filter( a -> AnalyzerAnnotation.class.equals( a.annotationType() ) )
							.map( a -> ( (AnalyzerAnnotation) a ).name() )
							.reduce( (a, b) -> {
								throw new IllegalStateException( "should not happen" );
							} );
					if ( analyzer.isPresent() ) {
						mapping.property( "text" )
								.fullTextField( "myText" ).analyzer( analyzer.get() );
					}
					else {
						mapping.property( "keyword" )
								.keywordField( "myKeyword" );
					}
				}
				else if ( annotatedElement.javaClass().getName().endsWith( "IndexedEntityType3" ) ) {
					mapping.property( "integer" ).genericField( "myInteger" );
				}
			}
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	private @interface AnalyzerAnnotation {

		String name();

	}


}
