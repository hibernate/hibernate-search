/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.annotation.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedType;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
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
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple type mapping will be applied as expected.
	 */
	@Test
	public void simple() {
		@Indexed(index = INDEX_NAME)
		@WorkingAnnotation
		class IndexedEntity {
			@DocumentId
			Integer id;
			String text;
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "myText", String.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
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
			@DocumentId
			Integer id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.annotationTypeContext( AnnotationWithEmptyProcessorRef.class )
						.failure( "Empty annotation processor reference in meta-annotation '"
								+ TypeMapping.class.getName() + "'" ) );
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
			@DocumentId
			Integer id;
		}
		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.annotationTypeContext( AnnotationWithProcessorWithDifferentAnnotationType.class )
						.failure( "Invalid annotation processor: '" + DifferentAnnotationType.Processor.TO_STRING + "'",
								"This processor expects annotations of a different type: '"
										+ DifferentAnnotationType.class.getName() + "'" ) );
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
		final String index4Name = "index4";
		final String index5Name = "index5";
		@Indexed(index = index1Name)
		@AnnotatedElementAwareAnnotation
		@AnalyzerAnnotation(name = "foo")
		class IndexedEntityType1 {
			@DocumentId
			Integer id;
			String text;
		}
		@Indexed(index = index2Name)
		@AnnotatedElementAwareAnnotation
		class IndexedEntityType2 {
			@DocumentId
			Integer id;
			String keyword;
		}
		@Indexed(index = index3Name)
		@AnnotatedElementAwareAnnotation
		class IndexedEntityType3 {
			@DocumentId
			Integer id;
			Integer integer;
		}
		@Indexed(index = index4Name)
		@AnnotatedElementAwareAnnotation
		@MultiFieldAnnotation.List({
				@MultiFieldAnnotation(name = "long1"),
				@MultiFieldAnnotation(name = "long2")
		})
		class IndexedEntityType4 {
			@DocumentId
			Integer id;
			Long longProperty;
		}
		@Indexed(index = index5Name)
		@AnnotatedElementAwareAnnotation
		@MultiFieldAnnotation(name = "long3")
		@MultiFieldAnnotation(name = "long4")
		class IndexedEntityType5 {
			@DocumentId
			Integer id;
			Long longProperty;
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
		backendMock.expectSchema( index4Name, b -> b
				.field( "long1", Long.class )
				.field( "long2", Long.class )
		);
		backendMock.expectSchema( index5Name, b -> b
				.field( "long3", Long.class )
				.field( "long4", Long.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.setup( IndexedEntityType1.class, IndexedEntityType2.class, IndexedEntityType3.class,
						IndexedEntityType4.class, IndexedEntityType5.class );
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
				else if ( annotatedElement.javaClass().getName().endsWith( "IndexedEntityType4" )
						|| annotatedElement.javaClass().getName().endsWith( "IndexedEntityType5" ) ) {
					annotatedElement.allAnnotations()
							.filter( a -> MultiFieldAnnotation.class.equals( a.annotationType() ) )
							.map( a -> ( (MultiFieldAnnotation) a ).name() )
							.forEach( name -> mapping.property( "longProperty" ).genericField( name ) );
				}
			}
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	private @interface AnalyzerAnnotation {

		String name();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	@Repeatable(MultiFieldAnnotation.List.class)
	// Must be public in order for Hibernate Search to be able to access List#value
	public @interface MultiFieldAnnotation {

		String name();

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ElementType.TYPE})
		@interface List {
			MultiFieldAnnotation[] value();
		}

	}

	@Test
	public void eventContext() {
		@Indexed(index = INDEX_NAME)
		@EventContextAwareAnnotation
		class IndexedEntityType {
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

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myText", String.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntityType.class );
		backendMock.verifyExpectationsMet();

		assertThat( EventContextAwareAnnotation.Processor.lastProcessedContext ).isNotNull();
		// Ideally we would not need a regexp here,
		// but the annotation can be rendered differently depending on the JDK in use...
		// See https://bugs.openjdk.java.net/browse/JDK-8282230
		assertThat( EventContextAwareAnnotation.Processor.lastProcessedContext.render() )
				.matches( "\\Qtype '" + IndexedEntityType.class.getName() + "', annotation '@\\E.*"
						+ EventContextAwareAnnotation.class.getSimpleName() + "\\Q()\\E'" );
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = EventContextAwareAnnotation.Processor.class))
	private @interface EventContextAwareAnnotation {

		class Processor implements TypeMappingAnnotationProcessor<EventContextAwareAnnotation> {
			static EventContext lastProcessedContext = null;

			@Override
			public void process(TypeMappingStep mapping, EventContextAwareAnnotation annotation,
					TypeMappingAnnotationProcessorContext context) {
				lastProcessedContext = context.eventContext();
				mapping.property( "text" ).genericField( "myText" );
			}
		}
	}

}
