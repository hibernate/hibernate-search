/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentReferenceProjection;
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
class ProjectionConstructorDocumentReferenceProjectionIT extends AbstractProjectionConstructorIT {

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
								f.documentReference()
						)
						.asList(),
				Arrays.asList(
						new NoArgMyProjection( reference( INDEX_NAME, "1" ) ),
						new NoArgMyProjection( reference( INDEX_NAME, "2" ) )
				)
		);
	}

	static class NoArgMyProjection {
		public final DocumentReference documentReference;

		@ProjectionConstructor
		public NoArgMyProjection(@DocumentReferenceProjection DocumentReference documentReference) {
			this.documentReference = documentReference;
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
								f.documentReference()
						)
						.asList(),
				Arrays.asList(
						new SupertypeMyProjection( reference( INDEX_NAME, "1" ) ),
						new SupertypeMyProjection( reference( INDEX_NAME, "2" ) )
				)
		);
	}

	static class SupertypeMyProjection {
		public final Object documentReference;

		@ProjectionConstructor
		public SupertypeMyProjection(@DocumentReferenceProjection Object documentReference) {
			this.documentReference = documentReference;
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
						.methodParameterContext( 0, "documentReference" )
						.failure(
								"Invalid projection definition for constructor parameter type '" + Integer.class.getName()
										+ "'",
								"This projection results in values of type '" + DocumentReference.class.getName() + "'" )
				);
	}

	static class InvalidTypeMyProjection {
		public final Integer documentReference;

		@ProjectionConstructor
		public InvalidTypeMyProjection(@DocumentReferenceProjection Integer documentReference) {
			this.documentReference = documentReference;
		}
	}

}
