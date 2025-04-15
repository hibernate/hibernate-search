/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.HighlightProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorHighlightProjectionIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void noArg() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NoArgMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, NoArgMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new NoArgMyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new NoArgMyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	static class NoArgMyProjection {
		public final List<String> text;

		@ProjectionConstructor
		public NoArgMyProjection(@HighlightProjection List<String> text) {
			this.text = text;
		}
	}

	@Test
	void path() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField(name = "myText")
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( PathMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, PathMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								f.highlight( "myText" )
						)
						.asList(),
				Arrays.asList(
						new PathMyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new PathMyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	static class PathMyProjection {
		public final List<String> text;

		@ProjectionConstructor
		public PathMyProjection(@HighlightProjection(path = "myText") List<String> text) {
			this.text = text;
		}
	}

	@Test
	void highlighter() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( HighlighterMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, HighlighterMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								f.highlight( "text" )
										.highlighter( "foo" )
						)
						.asList(),
				Arrays.asList(
						new HighlighterMyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new HighlighterMyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	static class HighlighterMyProjection {
		public final List<String> text;

		@ProjectionConstructor
		public HighlighterMyProjection(@HighlightProjection(highlighter = "foo") List<String> text) {
			this.text = text;
		}
	}

	@Test
	void superType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( SuperTypeMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, SuperTypeMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new SuperTypeMyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new SuperTypeMyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	static class SuperTypeMyProjection {
		public final List<CharSequence> text;

		@ProjectionConstructor
		public SuperTypeMyProjection(@HighlightProjection List<CharSequence> text) {
			this.text = text;
		}
	}

	@Test
	void singleValued() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( SingleValuedMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, SingleValuedMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1_term1" ),
						Arrays.asList( "result1_term1" )
				),
				f -> f.composite()
						.from(
								f.highlight( "text" ).single()
						)
						.asList(),
				Arrays.asList(
						new SingleValuedMyProjection( "result1_term1" ),
						new SingleValuedMyProjection( "result1_term1" )
				)
		);
	}

	static class SingleValuedMyProjection {
		public final String text;

		@ProjectionConstructor
		public SingleValuedMyProjection(@HighlightProjection String text) {
			this.text = text;
		}
	}

	// Technically this is not supported on the backend side,
	// but we'll test it just to be sure the mapper side works.
	@Test
	void inObjectField() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public Contained contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( InObjectFieldMyProjection.class, InObjectFieldMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectFieldMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ),
								Arrays.asList( Arrays.asList( "result1_term2", "result1_term3" ) ) ),
						Arrays.asList( Arrays.asList( "result2_term1" ), Arrays.asList( (Object) null ) ),
						Arrays.asList( Arrays.asList( "result3_term1" ), null )
				),
				f -> f.composite()
						.from(
								f.highlight( "text" ),
								f.object( "contained" )
										.from(
												f.highlight( "contained.text" )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectFieldMyProjection( Arrays.asList( "result1_term1", "result1_term2" ),
								new InObjectFieldMyInnerProjection( Arrays.asList( "result1_term2", "result1_term3" ) ) ),
						new InObjectFieldMyProjection( Arrays.asList( "result2_term1" ),
								new InObjectFieldMyInnerProjection( null ) ),
						new InObjectFieldMyProjection( Arrays.asList( "result3_term1" ), null )
				)
		);
	}

	static class InObjectFieldMyInnerProjection {
		public final List<String> text;

		@ProjectionConstructor
		public InObjectFieldMyInnerProjection(@HighlightProjection List<String> text) {
			this.text = text;
		}
	}

	static class InObjectFieldMyProjection {
		public final List<String> text;
		public final InObjectFieldMyInnerProjection contained;

		@ProjectionConstructor
		public InObjectFieldMyProjection(@HighlightProjection List<String> text, InObjectFieldMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void collection() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( CollectionMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, CollectionMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new CollectionMyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new CollectionMyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	static class CollectionMyProjection {
		public final Collection<String> text;

		@ProjectionConstructor
		public CollectionMyProjection(@HighlightProjection Collection<String> text) {
			this.text = text;
		}
	}

	@Test
	void iterable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( IterableMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, IterableMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new IterableMyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new IterableMyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	static class IterableMyProjection {
		public final Iterable<String> text;

		@ProjectionConstructor
		public IterableMyProjection(@HighlightProjection Iterable<String> text) {
			this.text = text;
		}
	}

	@Test
	void set() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( SetMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( SetMyProjection.class.getName() )
						.constructorContext( Set.class )
						.methodParameterContext( 0, "text" )
						.failure( "Invalid parameter type for projection constructor: java.util.Set<java.lang.String>",
								"When inferring the cardinality of inner projections from constructor parameters, multi-valued constructor parameters must be lists (java.util.List<...>) or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );

	}

	static class SetMyProjection {
		public final Set<String> text;

		@ProjectionConstructor
		public SetMyProjection(@HighlightProjection Set<String> text) {
			this.text = text;
		}
	}

	@Test
	void invalidElementType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( InvalidElementTypeMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( InvalidElementTypeMyProjection.class.getName() )
						.constructorContext( List.class )
						.methodParameterContext( 0, "text" )
						.failure( "Invalid multi-valued projection definition for constructor parameter type",
								"'java.util.List<java.lang.Integer>'",
								"This projection results in values of type 'java.lang.String'" ) );
	}

	static class InvalidElementTypeMyProjection {
		public final List<Integer> text;

		@ProjectionConstructor
		public InvalidElementTypeMyProjection(@HighlightProjection List<Integer> text) {
			this.text = text;
		}
	}

}
