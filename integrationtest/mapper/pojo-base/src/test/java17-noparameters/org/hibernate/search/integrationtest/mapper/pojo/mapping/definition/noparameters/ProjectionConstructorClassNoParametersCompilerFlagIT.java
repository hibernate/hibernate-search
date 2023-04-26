/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition.noparameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Parameter;

import org.hibernate.search.integrationtest.mapper.pojo.mapping.definition.AbstractProjectionConstructorIT;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProjectionConstructorClassNoParametersCompilerFlagIT extends AbstractProjectionConstructorIT {

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
	public void implicitInnerMapping() {
		@ProjectionConstructor
		class MyProjection {
			private final String someText;
			private final Integer someInteger;
			MyProjection(String someText, Integer someInteger) {
				this.someText = someText;
				this.someInteger = someInteger;
			}
		}
		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorClassNoParametersCompilerFlagIT.class, String.class, Integer.class )
						.methodParameterContext( 1 )
						.failure( "Missing parameter names in Java metadata for projection constructor",
								"When inferring inner projections from constructor parameters,"
										+ " constructor parameter names must be known",
								"Make sure this class was compiled with the '-parameters' compiler flag" )
						.methodParameterContext( 2 )
						.failure( "Missing parameter names in Java metadata for projection constructor",
								"When inferring inner projections from constructor parameters,"
										+ " constructor parameter names must be known",
								"Make sure this class was compiled with the '-parameters' compiler flag" ) );
	}

	@Indexed(index = INDEX_NAME)
	static class IndexedEntity {
		@DocumentId
		public Integer id;
		@FullTextField
		public String text;
		@GenericField
		public Integer integer;
	}

	static class ConstructorWithParameters {
		ConstructorWithParameters(int paramInt, String paramString) {
		}
	}
}
