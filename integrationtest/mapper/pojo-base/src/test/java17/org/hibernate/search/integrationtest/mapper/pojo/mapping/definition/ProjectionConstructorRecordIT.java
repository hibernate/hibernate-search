/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProjectionConstructorRecordIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void typeLevelAnnotation() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@ProjectionConstructor
		record MyProjection(String text, Integer integer) {
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );
		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.field( "integer", Integer.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", 1 ),
						new MyProjection( "result2", 2 ),
						new MyProjection( "result3", 3 )
				)
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4853")
	void constructorLevelAnnotation_canonical() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		record MyProjection(String text, Integer integer) {
			@ProjectionConstructor
			public MyProjection {
			}

			public MyProjection(String text, Integer integer, String somethingElse) {
				this( text, integer );
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );
		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.field( "integer", Integer.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", 1 ),
						new MyProjection( "result2", 2 ),
						new MyProjection( "result3", 3 )
				)
		);
	}

	@Test
	void constructorLevelAnnotation_nonCanonical() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@FullTextField
			public String otherText;
		}
		record MyProjection(String text, Integer integer) {
			public MyProjection {
			}

			@ProjectionConstructor
			public MyProjection(String text, Integer integer, String otherText) {
				this( text + otherText, integer );
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );
		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1, "otherText1" ),
						Arrays.asList( "result2", 2, "otherText2" ),
						Arrays.asList( "result3", 3, "otherText3" )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.field( "integer", Integer.class ),
								f.field( "otherText", String.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", 1, "otherText1" ),
						new MyProjection( "result2", 2, "otherText2" ),
						new MyProjection( "result3", 3, "otherText3" )
				)
		);
	}

	@Test
	void innerExplicit() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@FullTextField
			public String otherText;
		}
		@ProjectionConstructor
		record MyProjection(@IdProjection Integer someId, @FieldProjection String text, @FieldProjection Integer integer) {
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );
		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "1", "result1", 11 ),
						Arrays.asList( "2", "result2", 21 ),
						Arrays.asList( "3", "result3", 31 )
				),
				f -> f.composite()
						.from(
								f.id( Integer.class ),
								f.field( "text", String.class ),
								f.field( "integer", Integer.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( 1, "result1", 11 ),
						new MyProjection( 2, "result2", 21 ),
						new MyProjection( 3, "result3", 31 )
				)
		);
	}

}
