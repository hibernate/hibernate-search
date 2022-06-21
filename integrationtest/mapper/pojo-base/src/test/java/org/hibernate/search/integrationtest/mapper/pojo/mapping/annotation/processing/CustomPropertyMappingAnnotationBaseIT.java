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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedProperty;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
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
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple property mapping will be applied as expected.
	 */
	@Test
	public void simple() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@WorkingAnnotation
			String text;
		}

		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "myText", String.class )
		);

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
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
			@DocumentId
			@AnnotationWithEmptyProcessorRef
			Integer id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.annotationTypeContext( AnnotationWithEmptyProcessorRef.class )
						.failure( "Empty annotation processor reference in meta-annotation '"
								+ PropertyMapping.class.getName() + "'" ) );
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
			@DocumentId
			@AnnotationWithProcessorWithDifferentAnnotationType
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

	@Test
	public void annotatedElement() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@AnnotatedElementAwareAnnotation
			@AnalyzerAnnotation(name = "foo")
			String text;
			@AnnotatedElementAwareAnnotation
			String keyword;
			@AnnotatedElementAwareAnnotation
			Integer integer;
			@AnnotatedElementAwareAnnotation
			@AnalyzerAnnotation(name = "bar")
			Map<String, Integer> textToIntMap;
			@AnnotatedElementAwareAnnotation
			@AnalyzerAnnotation(name = "foobar")
			Collection<String> textCollection;
			@AnnotatedElementAwareAnnotation
			@MultiFieldAnnotation.List({
					@MultiFieldAnnotation(name = "long1"),
					@MultiFieldAnnotation(name = "long2")
			})
			Long longWithContainerAnnotation;
			@AnnotatedElementAwareAnnotation
			@MultiFieldAnnotation(name = "long3")
			@MultiFieldAnnotation(name = "long4")
			Long longWithRepeatedAnnotation;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myText", String.class, b2 -> b2.analyzerName( "foo" ) )
				.field( "myKeyword", String.class )
				.field( "myInteger", Integer.class )
				.field( "myTextMapKeys", String.class, b2 -> b2.multiValued( true ).analyzerName( "bar" ) )
				.field( "myTextCollection", String.class, b2 -> b2.multiValued( true ).analyzerName( "foobar" ) )
				.field( "long1", Long.class )
				.field( "long2", Long.class )
				.field( "long3", Long.class )
				.field( "long4", Long.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = AnnotatedElementAwareAnnotation.Processor.class))
	private @interface AnnotatedElementAwareAnnotation {
		class Processor implements PropertyMappingAnnotationProcessor<AnnotatedElementAwareAnnotation> {
			@Override
			public void process(PropertyMappingStep mapping, AnnotatedElementAwareAnnotation annotation,
					PropertyMappingAnnotationProcessorContext context) {
				MappingAnnotatedProperty annotatedElement = context.annotatedElement();
				if ( String.class.equals( annotatedElement.javaClass() ) ) {
					Optional<String> analyzer = annotatedElement.allAnnotations()
							.filter( a -> AnalyzerAnnotation.class.equals( a.annotationType() ) )
							.map( a -> ( (AnalyzerAnnotation) a ).name() )
							.reduce( (a, b) -> {
								throw new IllegalStateException( "should not happen" );
							} );
					if ( analyzer.isPresent() ) {
						assertThat( annotatedElement.name() ).isEqualTo( "text" );
						mapping.fullTextField( "myText" ).analyzer( analyzer.get() );
					}
					else {
						assertThat( annotatedElement.name() ).isEqualTo( "keyword" );
						mapping.keywordField( "myKeyword" );
					}
				}
				else if ( Integer.class.equals( annotatedElement.javaClass() ) ) {
					assertThat( annotatedElement.name() ).isEqualTo( "integer" );
					mapping.genericField( "myInteger" );
				}
				else if ( annotatedElement.javaClass(
						ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) )
						.filter( Predicate.isEqual( String.class ) ).isPresent() ) {
					assertThat( annotatedElement.name() ).isEqualTo( "textToIntMap" );
					Optional<String> analyzer = annotatedElement.allAnnotations()
							.filter( a -> AnalyzerAnnotation.class.equals( a.annotationType() ) )
							.map( a -> ( (AnalyzerAnnotation) a ).name() )
							.reduce( (a, b) -> {
								throw new IllegalStateException( "should not happen" );
							} );
					mapping.fullTextField( "myTextMapKeys" )
							.extractor( BuiltinContainerExtractors.MAP_KEY )
							.analyzer( analyzer.get() );
				}
				else if ( annotatedElement.javaClass( ContainerExtractorPath.defaultExtractors() )
						.filter( Predicate.isEqual( String.class ) ).isPresent() ) {
					assertThat( annotatedElement.name() ).isEqualTo( "textCollection" );
					Optional<String> analyzer = annotatedElement.allAnnotations()
							.filter( a -> AnalyzerAnnotation.class.equals( a.annotationType() ) )
							.map( a -> ( (AnalyzerAnnotation) a ).name() )
							.reduce( (a, b) -> {
								throw new IllegalStateException( "should not happen" );
							} );
					mapping.fullTextField( "myTextCollection" )
							.analyzer( analyzer.get() );
				}
				else if ( Long.class.equals( annotatedElement.javaClass() ) ) {
					assertThat( annotatedElement.name() ).startsWith( "longWith" );
					annotatedElement.allAnnotations()
							.filter( a -> MultiFieldAnnotation.class.equals( a.annotationType() ) )
							.map( a -> ( (MultiFieldAnnotation) a ).name() )
							.forEach( mapping::genericField );
				}
			}
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	private @interface AnalyzerAnnotation {

		String name();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@Repeatable(MultiFieldAnnotation.List.class)
	// Must be public in order for Hibernate Search to be able to access List#value
	public @interface MultiFieldAnnotation {

		String name();

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ElementType.FIELD, ElementType.METHOD})
		@interface List {
			MultiFieldAnnotation[] value();
		}

	}

	@Test
	public void eventContext() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntityType {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@EventContextAwareAnnotation
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
				.matches( "\\Qtype '" + IndexedEntityType.class.getName() + "', path '.text', annotation '@\\E.*"
						+ EventContextAwareAnnotation.class.getSimpleName() + "\\Q()\\E'" );
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.METHOD})
	@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = EventContextAwareAnnotation.Processor.class))
	private @interface EventContextAwareAnnotation {

		class Processor implements PropertyMappingAnnotationProcessor<EventContextAwareAnnotation> {
			static EventContext lastProcessedContext = null;

			@Override
			public void process(PropertyMappingStep mapping, EventContextAwareAnnotation annotation,
					PropertyMappingAnnotationProcessorContext context) {
				lastProcessedContext = context.eventContext();
				mapping.genericField( "myText" );
			}
		}
	}
}
