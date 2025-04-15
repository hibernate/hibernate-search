/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityReferenceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorEntityReferenceProjectionIT extends AbstractProjectionConstructorIT {

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
						Arrays.asList( reference( INDEX_NAME, "1" ) ),
						Arrays.asList( reference( INDEX_NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								f.entityReference()
						)
						.asList(),
				Arrays.asList(
						new NoArgMyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 1 ) ),
						new NoArgMyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 2 ) )
				)
		);
	}

	static class NoArgMyProjection {
		public final EntityReference entityReference;

		@ProjectionConstructor
		public NoArgMyProjection(@EntityReferenceProjection EntityReference entityReference) {
			this.entityReference = entityReference;
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
						Arrays.asList( reference( INDEX_NAME, "1" ) ),
						Arrays.asList( reference( INDEX_NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								f.entityReference()
						)
						.asList(),
				Arrays.asList(
						new SupertypeMyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 1 ) ),
						new SupertypeMyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 2 ) )
				)
		);
	}

	static class SupertypeMyProjection {
		public final Object entityReference;

		@ProjectionConstructor
		public SupertypeMyProjection(@EntityReferenceProjection Object entityReference) {
			this.entityReference = entityReference;
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
						.methodParameterContext( 0, "entityReference" )
						.failure(
								"Invalid projection definition for constructor parameter type '" + Integer.class.getName()
										+ "'",
								"This projection results in values of type '" + EntityReference.class.getName() + "'" )
				);
	}

	static class InvalidTypeMyProjection {
		public final Integer entityReference;

		@ProjectionConstructor
		public InvalidTypeMyProjection(@EntityReferenceProjection Integer entityReference) {
			this.entityReference = entityReference;
		}
	}

}
