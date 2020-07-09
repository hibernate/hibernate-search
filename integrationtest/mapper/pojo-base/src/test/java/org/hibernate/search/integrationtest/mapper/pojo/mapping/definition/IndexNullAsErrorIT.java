/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.assertj.core.api.Assertions;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IndexNullAsErrorIT<V, F> {

	private static final String FIELD_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_NAME;
	private static final String FIELD_INDEXNULLAS_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_INDEXNULLAS_NAME;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] types() {
		return PropertyTypeDescriptor.getAll().stream()
				// do not test types that do not have a default value bridge
				.filter( type -> type.getDefaultValueBridgeExpectations().isPresent() )
				.filter( type -> type.isNullable() )
				.map( type -> new Object[] { type, type.getDefaultValueBridgeExpectations() } )
				.toArray();
	}

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private DefaultValueBridgeExpectations<V, F> expectations;

	public IndexNullAsErrorIT(PropertyTypeDescriptor<V> typeDescriptor, Optional<DefaultValueBridgeExpectations<V, F>> expectations) {
		this.expectations = expectations.get();
	}

	@Test
	public void testParsingException() {
		String unparsableNullAsValue = expectations.getUnparsableNullAsValue();
		// Null means "there's no value I can't parse". Useful for the String type.
		Assume.assumeNotNull( unparsableNullAsValue );

		Assertions.assertThatThrownBy( () ->
				setupHelper.start().withConfiguration( c -> {
					c.addEntityType( expectations.getTypeWithValueBridge1() );
					TypeMappingStep typeMapping = c.programmaticMapping().type( expectations.getTypeWithValueBridge1() );
					typeMapping.indexed();
					typeMapping.property( FIELD_NAME ).genericField( FIELD_NAME );
					typeMapping.property( FIELD_NAME )
							.genericField( FIELD_INDEXNULLAS_NAME ).indexNullAs( unparsableNullAsValue );
				} ).setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH0005" )
				.hasMessageContaining( expectations.getTypeWithValueBridge1().getSimpleName() );

	}
}
