/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IndexNullAsErrorIT<V, F> {

	private static final String FIELD_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_NAME;
	private static final String FIELD_INDEXNULLAS_NAME =
			DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_INDEXNULLAS_NAME;

	public static List<? extends Arguments> params() {
		return PropertyTypeDescriptor.getAll().stream()
				.filter( type -> type.isNullable() )
				.map( type -> Arguments.of( type, type.getDefaultValueBridgeExpectations() ) )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void testParsingException(PropertyTypeDescriptor<V, F> typeDescriptor, DefaultValueBridgeExpectations<V, F> expectations) {
		String unparsableNullAsValue = expectations.getUnparsableNullAsValue();
		// Null means "there's no value I can't parse". Useful for the String type.
		assumeTrue( Objects.nonNull( unparsableNullAsValue ) );

		assertThatThrownBy( () -> setupHelper.start().withConfiguration( c -> {
			TypeMappingStep typeMapping = c.programmaticMapping().type( expectations.getTypeWithValueBridge1() );
			typeMapping.searchEntity();
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
