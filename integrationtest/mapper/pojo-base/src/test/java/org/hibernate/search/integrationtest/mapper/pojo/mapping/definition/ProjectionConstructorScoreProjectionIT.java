/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScoreProjection;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorScoreProjectionIT extends AbstractProjectionConstructorIT {

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
						Arrays.asList( 1.0f ),
						Arrays.asList( 2.0f )
				),
				f -> f.composite()
						.from(
								f.score()
						)
						.asList(),
				Arrays.asList(
						new NoArgMyProjection( 1.0f ),
						new NoArgMyProjection( 2.0f )
				)
		);
	}

	static class NoArgMyProjection {
		public final Float score;

		@ProjectionConstructor
		public NoArgMyProjection(@ScoreProjection Float score) {
			this.score = score;
		}
	}

	@Test
	void supertype() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( SupertypeMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, SupertypeMyProjection.class,
				Arrays.asList(
						Arrays.asList( 1.0f ),
						Arrays.asList( 2.0f )
				),
				f -> f.composite()
						.from(
								f.score()
						)
						.asList(),
				Arrays.asList(
						new SupertypeMyProjection( 1.0f ),
						new SupertypeMyProjection( 2.0f )
				)
		);
	}

	static class SupertypeMyProjection {
		public final Object score;

		@ProjectionConstructor
		public SupertypeMyProjection(@ScoreProjection Object score) {
			this.score = score;
		}
	}

	@Test
	void primitiveType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( PrimitiveTypeMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, PrimitiveTypeMyProjection.class,
				Arrays.asList(
						Arrays.asList( 1.0f ),
						Arrays.asList( 2.0f )
				),
				f -> f.composite()
						.from(
								f.score()
						)
						.asList(),
				Arrays.asList(
						new PrimitiveTypeMyProjection( 1.0f ),
						new PrimitiveTypeMyProjection( 2.0f )
				)
		);
	}

	static class PrimitiveTypeMyProjection {
		public final float score;

		@ProjectionConstructor
		public PrimitiveTypeMyProjection(@ScoreProjection float score) {
			this.score = score;
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
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( InvalidTypeMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( InvalidTypeMyProjection.class.getName() )
						.constructorContext( Integer.class )
						.methodParameterContext( 0, "score" )
						.failure(
								"Invalid projection definition for constructor parameter type '" + Integer.class.getName()
										+ "'",
								"This projection results in values of type '" + Float.class.getName() + "'" )
				);
	}

	static class InvalidTypeMyProjection {
		public final Integer score;

		@ProjectionConstructor
		public InvalidTypeMyProjection(@ScoreProjection Integer score) {
			this.score = score;
		}
	}
}
