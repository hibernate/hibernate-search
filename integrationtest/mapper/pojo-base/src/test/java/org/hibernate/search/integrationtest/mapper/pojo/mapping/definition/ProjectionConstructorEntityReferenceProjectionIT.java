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
		class MyProjection {
			public final EntityReference entityReference;

			@ProjectionConstructor
			public MyProjection(@EntityReferenceProjection EntityReference entityReference) {
				this.entityReference = entityReference;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( reference( INDEX_NAME, "1" ) ),
						Arrays.asList( reference( INDEX_NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.entityReference()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 1 ) ),
						new MyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 2 ) )
				)
		);
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
		class MyProjection {
			public final Object entityReference;

			@ProjectionConstructor
			public MyProjection(@EntityReferenceProjection Object entityReference) {
				this.entityReference = entityReference;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( reference( INDEX_NAME, "1" ) ),
						Arrays.asList( reference( INDEX_NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.entityReference()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 1 ) ),
						new MyProjection( PojoEntityReference.withDefaultName( IndexedEntity.class, 2 ) )
				)
		);
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
		class MyProjection {
			public final Integer entityReference;

			@ProjectionConstructor
			public MyProjection(@EntityReferenceProjection Integer entityReference) {
				this.entityReference = entityReference;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorEntityReferenceProjectionIT.class, Integer.class )
						.methodParameterContext( 1, "entityReference" )
						.failure(
								"Invalid projection definition for constructor parameter type '" + Integer.class.getName()
										+ "'",
								"This projection results in values of type '" + EntityReference.class.getName() + "'" )
				);
	}

}
