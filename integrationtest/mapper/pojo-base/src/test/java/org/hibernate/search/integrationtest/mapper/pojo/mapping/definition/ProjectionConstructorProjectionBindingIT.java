/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collections;

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
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test common use cases of the {@link ProjectionBinding} annotation.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorProjectionBindingIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void simple() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField(name = "myText")
			String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( SimpleMyProjection.class )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchProjection(
					INDEX_NAME,
					b -> {
						SearchProjectionFactory<?, ?> f = mapping.scope( IndexedEntity.class ).projection();
						b.projection( f.composite()
								.from(
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
					.select( SimpleMyProjection.class )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveFieldByFieldElementComparator()
					.containsExactly(
							new SimpleMyProjection( "hit1Text" ),
							new SimpleMyProjection( "hit2Text" )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	static class SimpleMyProjection {
		public final String text;

		@ProjectionConstructor
		public SimpleMyProjection(
				@ProjectionBinding(binder = @ProjectionBinderRef(type = WorkingProjectionBinder.class)) String text) {
			this.text = text;
		}
	}

	public static class WorkingProjectionBinder implements ProjectionBinder {
		@Override
		public void bind(ProjectionBindingContext context) {
			context.definition( String.class,
					(context1) -> context1.projection().field( "myText", String.class ).toProjection() );
		}
	}

	@Test
	void missingBinderReference() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField(name = "myText")
			String text;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MissingBinderReferenceMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MissingBinderReferenceMyProjection.class.getName() )
						.constructorContext( String.class )
						.methodParameterContext( 0, "text" )
						.annotationContextAnyParameters( ProjectionBinding.class )
						.failure( "Empty binder reference." )
				);
	}

	static class MissingBinderReferenceMyProjection {
		public final String text;

		@ProjectionConstructor
		public MissingBinderReferenceMyProjection(@ProjectionBinding(binder = @ProjectionBinderRef) String text) {
			this.text = text;
		}
	}

	@Test
	void params() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField(name = "myText")
			String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( ParamsMyProjection.class )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchProjection(
					INDEX_NAME,
					b -> {
						SearchProjectionFactory<?, ?> f = mapping.scope( IndexedEntity.class ).projection();
						b.projection( f.composite()
								.from(
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
					.select( ParamsMyProjection.class )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveFieldByFieldElementComparator()
					.containsExactly(
							new ParamsMyProjection( "hit1Text" ),
							new ParamsMyProjection( "hit2Text" )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	static class ParamsMyProjection {
		public final String text;

		@ProjectionConstructor
		public ParamsMyProjection(@ProjectionBinding(binder = @ProjectionBinderRef(type = ParametricBinder.class,
				params = @Param(name = "fieldPath", value = "myText"))) String text) {
			this.text = text;
		}
	}

	@Test
	void params_paramNotDefined() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField
			String text;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( Params_paramNotDefinedMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Params_paramNotDefinedMyProjection.class.getName() )
						.constructorContext( String.class )
						.methodParameterContext( 0, "text" )
						.failure( "Param with name 'fieldPath' has not been defined for the binder." )
				);
	}

	static class Params_paramNotDefinedMyProjection {
		public final String text;

		@ProjectionConstructor
		public Params_paramNotDefinedMyProjection(
				@ProjectionBinding(binder = @ProjectionBinderRef(type = ParametricBinder.class)) String text) {
			this.text = text;
		}
	}

	@Test
	void params_paramDefinedTwice() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField
			String text;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( Params_paramDefinedTwiceMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Params_paramDefinedTwiceMyProjection.class.getName() )
						.constructorContext( String.class )
						.methodParameterContext( 0, "text" )
						.annotationContextAnyParameters( ProjectionBinding.class )
						.failure( "Conflicting usage of @Param annotation for parameter name: 'fieldPath'. " +
								"Can't assign both value 'foo' and 'foo'" )
				);
	}

	static class Params_paramDefinedTwiceMyProjection {
		public final String text;

		@ProjectionConstructor
		public Params_paramDefinedTwiceMyProjection(
				@ProjectionBinding(binder = @ProjectionBinderRef(type = ParametricBinder.class,
						params = {
								@Param(name = "fieldPath", value = "foo"),
								@Param(name = "fieldPath", value = "foo") })) String text) {
			this.text = text;
		}
	}

	@Test
	void params_programmaticMapping() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Long id;
			@GenericField(name = "myText")
			String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withConfiguration( builder -> {
					TypeMappingStep indexedEntity =
							builder.programmaticMapping().type( Params_programmaticMappingMyProjection.class );
					indexedEntity.mainConstructor()
							.projectionConstructor()
							.parameter( 0 )
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
					.select( Params_programmaticMappingMyProjection.class )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveFieldByFieldElementComparator()
					.containsExactly(
							new Params_programmaticMappingMyProjection( "hit1Text" ),
							new Params_programmaticMappingMyProjection( "hit2Text" )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	static class Params_programmaticMappingMyProjection {
		public final String text;

		public Params_programmaticMappingMyProjection(String text) {
			this.text = text;
		}
	}

	public static class ParametricBinder implements ProjectionBinder {
		@Override
		public void bind(ProjectionBindingContext context) {
			String fieldPath = context.param( "fieldPath", String.class );
			context.definition( String.class,
					(context1) -> context1.projection().field( fieldPath, String.class ).toProjection() );
		}
	}
}
