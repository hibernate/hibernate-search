/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collections;

import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.mapper.pojo.search.definition.mapping.annotation.ProjectionBinderRef;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link ProjectionBinding} annotation.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-4574")
public class ProjectionConstructorProjectionBindingIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	protected final ProjectionFinalStep<?> dummyProjectionForEnclosingClassInstance(SearchProjectionFactory<?, ?> f) {
		return f.constant( null );
	}

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
			public MyProjection(
					@ProjectionBinding(binder = @ProjectionBinderRef(type = WorkingProjectionBinder.class)) String text) {
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

	public static class WorkingProjectionBinder implements ProjectionBinder {
		@Override
		public void bind(ProjectionBindingContext context) {
			context.definition( String.class,
					(factory, context1) -> factory.field( "myText", String.class ).toProjection() );
		}
	}

	@Test
	public void missingBinderReference() {
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
			public MyProjection(@ProjectionBinding(binder = @ProjectionBinderRef) String text) {
				this.text = text;
			}
		}
		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorProjectionBindingIT.class, String.class )
						.methodParameterContext( 1, "text" )
						.annotationContextAnyParameters( ProjectionBinding.class )
						.failure( "Empty binder reference." )
				);
	}

	@Test
	public void params() {
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
			public MyProjection(@ProjectionBinding(binder = @ProjectionBinderRef(type = ParametricBinder.class,
					params = @Param(name = "fieldPath", value = "myText"))) String text) {
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

	@Test
	public void params_paramNotDefined() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField
			String text;
		}
		class MyProjection {
			public final String text;

			@ProjectionConstructor
			public MyProjection(@ProjectionBinding(binder = @ProjectionBinderRef(type = ParametricBinder.class)) String text) {
				this.text = text;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorProjectionBindingIT.class, String.class )
						.methodParameterContext( 1, "text" )
						.failure( "Param with name 'fieldPath' has not been defined for the binder." )
				);
	}

	@Test
	public void params_paramDefinedTwice() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField
			String text;
		}
		class MyProjection {
			public final String text;

			@ProjectionConstructor
			public MyProjection(@ProjectionBinding(binder = @ProjectionBinderRef(type = ParametricBinder.class,
					params = {
							@Param(name = "fieldPath", value = "foo"),
							@Param(name = "fieldPath", value = "foo") })) String text) {
				this.text = text;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorProjectionBindingIT.class, String.class )
						.methodParameterContext( 1, "text" )
						.annotationContextAnyParameters( ProjectionBinding.class )
						.failure( "Conflicting usage of @Param annotation for parameter name: 'fieldPath'. " +
								"Can't assign both value 'foo' and 'foo'" )
				);
	}

	@Test
	public void params_programmaticMapping() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField(name = "myText")
			String text;
		}
		class MyProjection {
			public final String text;

			public MyProjection(String text) {
				this.text = text;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withConfiguration( builder -> {
					TypeMappingStep indexedEntity = builder.programmaticMapping().type( MyProjection.class );
					indexedEntity.mainConstructor()
							.projectionConstructor()
							.parameter( 1 )
							.projection( new ParametricBinder(), Collections.singletonMap( "fieldPath", "myText" ) );
				} )
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

	public static class ParametricBinder implements ProjectionBinder {
		@Override
		public void bind(ProjectionBindingContext context) {
			String fieldPath = context.param( "fieldPath", String.class );
			context.definition( String.class,
					(factory, context1) -> factory.field( fieldPath, String.class ).toProjection() );
		}
	}
}
