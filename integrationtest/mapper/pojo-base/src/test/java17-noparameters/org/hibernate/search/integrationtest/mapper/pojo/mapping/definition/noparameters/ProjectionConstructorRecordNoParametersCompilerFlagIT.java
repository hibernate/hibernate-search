/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition.noparameters;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import org.hibernate.search.integrationtest.mapper.pojo.mapping.definition.AbstractProjectionConstructorIT;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProjectionConstructorRecordNoParametersCompilerFlagIT extends AbstractProjectionConstructorIT {

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Before
	public void sourcesCompiledWithoutParametersFlag() {
		assertThat( ConstructorWithParameters.class.getDeclaredConstructors()[0].getParameters() )
				.withFailMessage( "This test only works if compiled *without* the '-parameters' compiler flag." )
				.extracting( Parameter::isNamePresent )
				.containsOnly( Boolean.FALSE );
	}

	@Test
	public void typeLevelAnnotation() {
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
		record MyProjection(String text, Integer integer) { }

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
	public void constructorLevelAnnotation_canonical() {
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

	static class ConstructorWithParameters {
		ConstructorWithParameters(int paramInt, String paramString) {
		}
	}
}
