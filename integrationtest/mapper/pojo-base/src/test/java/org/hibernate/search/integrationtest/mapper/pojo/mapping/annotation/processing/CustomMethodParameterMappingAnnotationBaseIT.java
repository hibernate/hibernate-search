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
import java.util.Collections;

import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedMethodParameter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of (custom) method parameter mapping annotations.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-4574")
public class CustomMethodParameterMappingAnnotationBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	protected final ProjectionFinalStep<?> dummyProjectionForEnclosingClassInstance(SearchProjectionFactory<?, ?> f) {
		return f.constant( null );
	}

	/**
	 * Basic test checking that a simple constructor mapping will be applied as expected.
	 */
	@Test
	public void simple() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField(name = "myText")
			String text;
		}
		class MyProjection {
			public final String text;

			@ProjectionConstructor
			public MyProjection(@WorkingAnnotation String text) {
				this.text = text;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchProjection(
					INDEX_NAME,
					b -> {
						SearchProjectionFactory<?, ?> f = mapping.scope( IndexedEntity.class ).projection();
						b.projection( f.composite()
								.from(
										dummyProjectionForEnclosingClassInstance( f ),
										f.field( "myText", String.class )
								)
								.asList() );
					},
					StubSearchWorkBehavior.of(
							2,
							Collections.singletonList( "hit1Text" ),
							Collections.singletonList( "hit2Text" )
					)
			);

			assertThat( session.search( IndexedEntity.class )
					.select( MyProjection.class )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveFieldByFieldElementComparator()
					.containsExactly(
							new MyProjection( "hit1Text" ),
							new MyProjection( "hit2Text" )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(type = WorkingAnnotation.Processor.class))
	private @interface WorkingAnnotation {
		class Processor implements MethodParameterMappingAnnotationProcessor<WorkingAnnotation> {
			@Override
			public void process(MethodParameterMappingStep mapping, WorkingAnnotation annotation,
					MethodParameterMappingAnnotationProcessorContext context) {
				mapping.projection( bindingContext -> {
					bindingContext.definition( String.class,
							(factory, definitionContext) -> factory.field( "myText", String.class ).toProjection() );
				} );
			}
		}
	}

	@Test
	public void missingProcessorReference() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Long id;

			public IndexedEntity(@AnnotationWithEmptyProcessorRef Long id) {
				this.id = id;
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.annotationTypeContext( AnnotationWithEmptyProcessorRef.class )
						.failure( "Empty annotation processor reference in meta-annotation '"
								+ MethodParameterMapping.class.getName() + "'" ) );
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef())
	private @interface AnnotationWithEmptyProcessorRef {
	}

	@Test
	public void invalidAnnotationType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Long id;

			public IndexedEntity(@AnnotationWithProcessorWithDifferentAnnotationType Long id) {
				this.id = id;
			}
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
	@Target(ElementType.PARAMETER)
	@MethodParameterMapping(
			processor = @MethodParameterMappingAnnotationProcessorRef(type = DifferentAnnotationType.Processor.class))
	private @interface AnnotationWithProcessorWithDifferentAnnotationType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	private @interface DifferentAnnotationType {
		class Processor
				implements MethodParameterMappingAnnotationProcessor<
						CustomMethodParameterMappingAnnotationBaseIT.DifferentAnnotationType> {
			public static final String TO_STRING = "DifferentAnnotationType.Processor";

			@Override
			public void process(MethodParameterMappingStep mapping,
					CustomMethodParameterMappingAnnotationBaseIT.DifferentAnnotationType annotation,
					MethodParameterMappingAnnotationProcessorContext context) {
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
		}
		class MyProjection {
			@ProjectionConstructor
			public MyProjection(
					@AnnotatedElementAwareAnnotation @OtherAnnotationForAnnotatedElementAwareAnnotation(
							name = "nonRepeatable") String paramWithOtherAnnotation,
					@AnnotatedElementAwareAnnotation @RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation.List({
							@RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation(name = "explicitRepeatable1"),
							@RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation(name = "explicitRepeatable2")
					}) String paramWithExplicitRepeatableOtherAnnotation,
					@AnnotatedElementAwareAnnotation @RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation(
							name = "implicitRepeatable1") @RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation(
									name = "implicitRepeatable2") String paramWithImplicitRepeatableOtherAnnotation) {
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		assertThat( counters.get( AnnotatedElementAwareAnnotation.CONSTRUCTOR_PARAMETER_WITH_OTHER_ANNOTATION ) )
				.isEqualTo( 1 );
		assertThat( counters
				.get( AnnotatedElementAwareAnnotation.CONSTRUCTOR_PARAMETER_WITH_EXPLICIT_REPEATABLE_OTHER_ANNOTATION ) )
				.isEqualTo( 1 );
		assertThat( counters
				.get( AnnotatedElementAwareAnnotation.CONSTRUCTOR_PARAMETER_WITH_IMPLICIT_REPEATABLE_OTHER_ANNOTATION ) )
				.isEqualTo( 1 );
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@MethodParameterMapping(
			processor = @MethodParameterMappingAnnotationProcessorRef(type = AnnotatedElementAwareAnnotation.Processor.class))
	private @interface AnnotatedElementAwareAnnotation {
		StaticCounters.Key CONSTRUCTOR_PARAMETER_WITH_OTHER_ANNOTATION = StaticCounters.createKey();
		StaticCounters.Key CONSTRUCTOR_PARAMETER_WITH_EXPLICIT_REPEATABLE_OTHER_ANNOTATION = StaticCounters.createKey();
		StaticCounters.Key CONSTRUCTOR_PARAMETER_WITH_IMPLICIT_REPEATABLE_OTHER_ANNOTATION = StaticCounters.createKey();

		class Processor
				implements MethodParameterMappingAnnotationProcessor<
						CustomMethodParameterMappingAnnotationBaseIT.AnnotatedElementAwareAnnotation> {
			@Override
			public void process(MethodParameterMappingStep mapping,
					CustomMethodParameterMappingAnnotationBaseIT.AnnotatedElementAwareAnnotation annotation,
					MethodParameterMappingAnnotationProcessorContext context) {
				MappingAnnotatedMethodParameter annotatedElement = context.annotatedElement();
				if ( annotatedElement.name().get().equals( "paramWithOtherAnnotation" ) ) {
					assertThat( annotatedElement.allAnnotations()
							.filter( a -> OtherAnnotationForAnnotatedElementAwareAnnotation.class.equals( a.annotationType() ) )
							.map( a -> ( (OtherAnnotationForAnnotatedElementAwareAnnotation) a ).name() )
							.toArray() )
							.containsExactlyInAnyOrder( "nonRepeatable" );
					StaticCounters.get().increment( CONSTRUCTOR_PARAMETER_WITH_OTHER_ANNOTATION );
				}
				else if ( annotatedElement.name().get().equals( "paramWithExplicitRepeatableOtherAnnotation" ) ) {
					assertThat( annotatedElement.allAnnotations()
							.filter( a -> RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation.class
									.equals( a.annotationType() ) )
							.map( a -> ( (RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation) a ).name() )
							.toArray() )
							.containsExactlyInAnyOrder( "explicitRepeatable1", "explicitRepeatable2" );
					StaticCounters.get().increment( CONSTRUCTOR_PARAMETER_WITH_EXPLICIT_REPEATABLE_OTHER_ANNOTATION );
				}
				else if ( annotatedElement.name().get().equals( "paramWithImplicitRepeatableOtherAnnotation" ) ) {
					assertThat( annotatedElement.allAnnotations()
							.filter( a -> RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation.class
									.equals( a.annotationType() ) )
							.map( a -> ( (RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation) a ).name() )
							.toArray() )
							.containsExactlyInAnyOrder( "implicitRepeatable1", "implicitRepeatable2" );
					StaticCounters.get().increment( CONSTRUCTOR_PARAMETER_WITH_IMPLICIT_REPEATABLE_OTHER_ANNOTATION );
				}
			}
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	private @interface OtherAnnotationForAnnotatedElementAwareAnnotation {

		String name();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@Repeatable(RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation.List.class)
	// Must be public in order for Hibernate Search to be able to access List#value
	public @interface RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation {

		String name();

		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.PARAMETER)
		@interface List {
			RepeatableOtherAnnotationForAnnotatedElementAwareAnnotation[] value();
		}

	}

	@Test
	public void eventContext() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntityType {
			@DocumentId
			Integer id;

			@ProjectionConstructor
			IndexedEntityType(@EventContextAwareAnnotation String text) {
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntityType.class );
		backendMock.verifyExpectationsMet();

		assertThat( EventContextAwareAnnotation.Processor.lastProcessedContext ).isNotNull();
		// Ideally we would not need a regexp here,
		// but the annotation can be rendered differently depending on the JDK in use...
		// See https://bugs.openjdk.java.net/browse/JDK-8282230
		assertThat( EventContextAwareAnnotation.Processor.lastProcessedContext.render() )
				.matches( "\\Qtype '" + IndexedEntityType.class.getName() + "', constructor with parameter types ["
						+ CustomMethodParameterMappingAnnotationBaseIT.class.getName() // Implicit parameter because we're declaring a nested class
						+ ", " + String.class.getName() + "]"
						+ ", parameter at index 1 (text)"
						+ ", annotation '@\\E.*"
						+ EventContextAwareAnnotation.class.getSimpleName() + "\\Q()\\E'" );
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@MethodParameterMapping(
			processor = @MethodParameterMappingAnnotationProcessorRef(type = EventContextAwareAnnotation.Processor.class))
	private @interface EventContextAwareAnnotation {

		class Processor
				implements MethodParameterMappingAnnotationProcessor<
						CustomMethodParameterMappingAnnotationBaseIT.EventContextAwareAnnotation> {
			static EventContext lastProcessedContext = null;

			@Override
			public void process(MethodParameterMappingStep mapping,
					CustomMethodParameterMappingAnnotationBaseIT.EventContextAwareAnnotation annotation,
					MethodParameterMappingAnnotationProcessorContext context) {
				lastProcessedContext = context.eventContext();
			}
		}
	}
}
