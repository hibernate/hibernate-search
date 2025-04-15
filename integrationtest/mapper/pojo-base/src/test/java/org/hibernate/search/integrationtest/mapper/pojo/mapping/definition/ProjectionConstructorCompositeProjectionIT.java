/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.CompositeProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
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
class ProjectionConstructorCompositeProjectionIT extends AbstractProjectionConstructorIT {

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
			@FullTextField
			public String text2;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NoArgMyProjection.class, NoArgMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, NoArgMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", 1 ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.composite()
										.from(
												f.field( "text2", String.class ),
												f.field( "integer", Integer.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new NoArgMyProjection( "result1", new NoArgMyInnerProjection( "result1_1", 1 ) ),
						new NoArgMyProjection( "result2", new NoArgMyInnerProjection( null, null ) )
				)
		);
	}

	static class NoArgMyInnerProjection {
		public final String text2;
		public final Integer integer;

		@ProjectionConstructor
		public NoArgMyInnerProjection(String text2, Integer integer) {
			this.text2 = text2;
			this.integer = integer;
		}
	}

	static class NoArgMyProjection {
		public final String text;
		public final NoArgMyInnerProjection composite;

		@ProjectionConstructor
		public NoArgMyProjection(String text, @CompositeProjection NoArgMyInnerProjection composite) {
			this.text = text;
			this.composite = composite;
		}
	}

	@Test
	void invalidType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@FullTextField
			public String text2;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( InvalidTypeMyProjection.class, InvalidTypeMyNonProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( InvalidTypeMyProjection.class.getName() )
						.constructorContext( InvalidTypeMyNonProjection.class )
						.methodParameterContext( 0, "composite" )
						.failure( "Invalid object class for projection",
								InvalidTypeMyNonProjection.class.getName(),
								"Make sure that this class is mapped correctly, "
										+ "either through annotations (@ProjectionConstructor) or programmatic mapping" ) );
	}

	static class InvalidTypeMyNonProjection {
		public final String text2;
		public final Integer integer;

		public InvalidTypeMyNonProjection() {
			this.text2 = "foo";
			this.integer = 42;
		}

		public InvalidTypeMyNonProjection(String text2, Integer integer) {
			this.text2 = text2;
			this.integer = integer;
		}
	}

	static class InvalidTypeMyProjection {
		public final InvalidTypeMyNonProjection composite;

		@ProjectionConstructor
		public InvalidTypeMyProjection(@CompositeProjection InvalidTypeMyNonProjection composite) {
			this.composite = composite;
		}
	}

	@Test
	void inObjectField() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@FullTextField
			public String text2;
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
				.withAnnotatedTypes( InObjectFieldMyProjection.class, InObjectFieldMyInnerProjectionLevel1.class,
						InObjectFieldMyInnerProjectionLevel2.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectFieldMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", Arrays.asList( "result1_1_1", 1 ) ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", Arrays.asList( "result3_1", Arrays.asList( null, null ) ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												f.composite()
														.from(
																f.field( "contained.text2", String.class ),
																f.field( "contained.integer", Integer.class )
														)
														.asList()
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectFieldMyProjection( "result1", new InObjectFieldMyInnerProjectionLevel1( "result1_1",
								new InObjectFieldMyInnerProjectionLevel2( "result1_1_1", 1 ) ) ),
						new InObjectFieldMyProjection( "result2", new InObjectFieldMyInnerProjectionLevel1( null, null ) ),
						new InObjectFieldMyProjection( "result3", new InObjectFieldMyInnerProjectionLevel1( "result3_1",
								new InObjectFieldMyInnerProjectionLevel2( null, null ) ) )
				)
		);
	}

	static class InObjectFieldMyInnerProjectionLevel2 {
		public final String text2;
		public final Integer integer;

		@ProjectionConstructor
		public InObjectFieldMyInnerProjectionLevel2(String text2, Integer integer) {
			this.text2 = text2;
			this.integer = integer;
		}
	}

	static class InObjectFieldMyInnerProjectionLevel1 {
		public final String text;
		public final InObjectFieldMyInnerProjectionLevel2 composite;

		@ProjectionConstructor
		public InObjectFieldMyInnerProjectionLevel1(String text,
				@CompositeProjection InObjectFieldMyInnerProjectionLevel2 composite) {
			this.text = text;
			this.composite = composite;
		}
	}

	static class InObjectFieldMyProjection {
		public final String text;
		public final InObjectFieldMyInnerProjectionLevel1 contained;

		@ProjectionConstructor
		public InObjectFieldMyProjection(String text, InObjectFieldMyInnerProjectionLevel1 contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void multiValued() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@FullTextField
			public String text2;
			@GenericField
			public Integer integer;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MultiValuedMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MultiValuedMyProjection.class.getName() )
						.constructorContext( String.class, List.class )
						.methodParameterContext( 1, "composite" )
						.failure( "Invalid object class for projection",
								List.class.getName(),
								"Make sure that this class is mapped correctly, "
										+ "either through annotations (@ProjectionConstructor) or programmatic mapping" ) );
	}

	static class MultiValuedMyInnerProjection {
		public final String text2;
		public final Integer integer;

		@ProjectionConstructor
		public MultiValuedMyInnerProjection(String text2, Integer integer) {
			this.text2 = text2;
			this.integer = integer;
		}
	}

	static class MultiValuedMyProjection {
		public final String text;
		public final List<MultiValuedMyInnerProjection> composite;

		@ProjectionConstructor
		public MultiValuedMyProjection(String text, @CompositeProjection List<MultiValuedMyInnerProjection> composite) {
			this.text = text;
			this.composite = composite;
		}
	}
}
