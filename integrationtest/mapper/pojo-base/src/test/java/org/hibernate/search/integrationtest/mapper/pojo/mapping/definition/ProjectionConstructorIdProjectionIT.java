/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4574")
public class ProjectionConstructorIdProjectionIT extends AbstractProjectionConstructorIT {

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void noArg() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer identifier; // Not the ID -- we're trying to confuse Hibernate Search.
		}
		class MyProjection {
			public final Integer identifier;

			@ProjectionConstructor
			public MyProjection(@IdProjection Integer identifier) {
				this.identifier = identifier;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "11" ),
						Arrays.asList( "21" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.id( Integer.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( 11 ),
						new MyProjection( 21 )
				)
		);
	}

	@Test
	public void supertype() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final Object id;

			@ProjectionConstructor
			public MyProjection(@IdProjection Object id) {
				this.id = id;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "11" ),
						Arrays.asList( "21" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.id( Object.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( 11 ),
						new MyProjection( 21 )
				)
		);
	}

	@Test
	public void primitiveType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public int id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final int id;

			@ProjectionConstructor
			public MyProjection(@IdProjection int id) {
				this.id = id;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "11" ),
						Arrays.asList( "21" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.id( Integer.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( 11 ),
						new MyProjection( 21 )
				)
		);
	}

}
