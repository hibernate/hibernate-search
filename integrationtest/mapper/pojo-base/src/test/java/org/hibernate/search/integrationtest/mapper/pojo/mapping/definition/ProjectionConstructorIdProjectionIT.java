/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorIdProjectionIT extends AbstractProjectionConstructorIT {

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
			@GenericField
			public Integer identifier; // Not the ID -- we're trying to confuse Hibernate Search.
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NoArgMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, NoArgMyProjection.class,
				Arrays.asList(
						Arrays.asList( "11" ),
						Arrays.asList( "21" )
				),
				f -> f.composite()
						.from(
								f.id( Integer.class )
						)
						.asList(),
				Arrays.asList(
						new NoArgMyProjection( 11 ),
						new NoArgMyProjection( 21 )
				)
		);
	}

	static class NoArgMyProjection {
		public final Integer identifier;

		@ProjectionConstructor
		public NoArgMyProjection(@IdProjection Integer identifier) {
			this.identifier = identifier;
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
						Arrays.asList( "11" ),
						Arrays.asList( "21" )
				),
				f -> f.composite()
						.from(
								f.id( Object.class )
						)
						.asList(),
				Arrays.asList(
						new SupertypeMyProjection( 11 ),
						new SupertypeMyProjection( 21 )
				)
		);
	}

	static class SupertypeMyProjection {
		public final Object id;

		@ProjectionConstructor
		public SupertypeMyProjection(@IdProjection Object id) {
			this.id = id;
		}
	}

	@Test
	void primitiveType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public int id;
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
						Arrays.asList( "11" ),
						Arrays.asList( "21" )
				),
				f -> f.composite()
						.from(
								f.id( Integer.class )
						)
						.asList(),
				Arrays.asList(
						new PrimitiveTypeMyProjection( 11 ),
						new PrimitiveTypeMyProjection( 21 )
				)
		);
	}

	static class PrimitiveTypeMyProjection {
		public final int id;

		@ProjectionConstructor
		public PrimitiveTypeMyProjection(@IdProjection int id) {
			this.id = id;
		}
	}

}
