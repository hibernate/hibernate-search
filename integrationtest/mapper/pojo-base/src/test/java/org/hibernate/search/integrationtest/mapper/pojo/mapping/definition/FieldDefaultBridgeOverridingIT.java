/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test overriding default value bridges for the {@code @GenericField} annotation,
 * for example assigning a different default value bridge for properties of type {@link String}.
 */
@TestForIssue(jiraKey = "HSEARCH-3096")
class FieldDefaultBridgeOverridingIT<V, F> {
	private static final String FIELD_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_NAME;
	private static final String FIELD_INDEXNULLAS_NAME =
			DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_INDEXNULLAS_NAME;

	public static List<? extends Arguments> params() {
		return PropertyTypeDescriptor.getAll().stream()
				// these tests cannot work with vector fields.
				.filter( Predicate.not( PropertyTypeDescriptor::isVectorType ) )
				.map( type -> Arguments.of( type, type.getDefaultValueBridgeExpectations() ) )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;
	private StubIndexNode indexField;

	public void setup(PropertyTypeDescriptor<V, F> typeDescriptor, DefaultValueBridgeExpectations<V, F> expectations) {
		backendMock.expectSchema(
				DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME, b -> {
					b.field( FIELD_NAME, FieldTypeForOverridingDefaultBridge.class, f -> {
						if ( typeDescriptor.isVectorType() ) {
							f.dimension( PropertyTypeDescriptor.VECTOR_DIMENSION );
						}
					} );
					if ( typeDescriptor.isNullable() ) {
						b.field( FIELD_INDEXNULLAS_NAME, FieldTypeForOverridingDefaultBridge.class,
								f -> {
									f.indexNullAs( new FieldTypeForOverridingDefaultBridge( "NULL_AS" ) );
									if ( typeDescriptor.isVectorType() ) {
										f.dimension( PropertyTypeDescriptor.VECTOR_DIMENSION );
									}
								} );
					}
				},
				indexModel -> this.indexField = indexModel.fieldOrNull( FIELD_NAME )
		);
		mapping = setupHelper.start()
				.withAnnotatedTypes( expectations.getTypeWithValueBridge1() )
				.withConfiguration( b -> {
					b.programmaticMapping().type( expectations.getTypeWithValueBridge1() )
							.searchEntity().name( DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME );
				} )
				// HERE we override the default bridge for the type being tested.
				.withConfiguration( builder -> builder.bridges().exactType( typeDescriptor.getJavaType() )
						.valueBridge( new OverridingDefaultBridge<>() ) )
				.setup();
		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void indexing(PropertyTypeDescriptor<V, F> typeDescriptor, DefaultValueBridgeExpectations<V, F> expectations) {
		setup( typeDescriptor, expectations );
		try ( SearchSession session = mapping.createSession() ) {
			Object entity = expectations.instantiateTypeWithValueBridge1( 1, getPropertyValue( typeDescriptor ) );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME )
					.add( "1", b -> {
						b.field( FIELD_NAME, getFieldValue( typeDescriptor ) );

						if ( typeDescriptor.isNullable() ) {
							// The stub backend does not implement the 'indexNullAs' option
							b.field( FIELD_INDEXNULLAS_NAME, new FieldTypeForOverridingDefaultBridge( null ) );
						}
					} );
		}
		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projection(PropertyTypeDescriptor<V, F> typeDescriptor, DefaultValueBridgeExpectations<V, F> expectations) {
		setup( typeDescriptor, expectations );
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<V> query = session.search( expectations.getTypeWithValueBridge1() )
					.select( f -> f.field( FIELD_NAME, typeDescriptor.getBoxedJavaType() ) )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME,
					StubSearchWorkBehavior.of(
							1L,
							getFieldValue( typeDescriptor )
					)
			);

			assertThat( query.fetchAll().hits() ).containsExactly( getPropertyValue( typeDescriptor ) );
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void dslConverter(PropertyTypeDescriptor<V, F> typeDescriptor, DefaultValueBridgeExpectations<V, F> expectations) {
		setup( typeDescriptor, expectations );
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		DslConverter<V, ?> dslConverter =
				(DslConverter<V, ?>) indexField.toValueField().type().dslConverter();
		ToDocumentValueConvertContext toDocumentConvertContext =
				new ToDocumentValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );

		// The overriden default bridge must be used by the DSL converter
		assertThat( dslConverter.toDocumentValue( getPropertyValue( typeDescriptor ), toDocumentConvertContext ) )
				.isEqualTo( getFieldValue( typeDescriptor ) );
		assertThat( dslConverter.unknownTypeToDocumentValue( getPropertyValue( typeDescriptor ), toDocumentConvertContext ) )
				.isEqualTo( getFieldValue( typeDescriptor ) );
	}

	private V getPropertyValue(PropertyTypeDescriptor<V, F> typeDescriptor) {
		return typeDescriptor.values().entityModelValues.get( 0 );
	}

	private FieldTypeForOverridingDefaultBridge getFieldValue(PropertyTypeDescriptor<V, F> typeDescriptor) {
		return new FieldTypeForOverridingDefaultBridge( getPropertyValue( typeDescriptor ) );
	}

	private static class FieldTypeForOverridingDefaultBridge {
		private final Object originalPropertyValue;

		private FieldTypeForOverridingDefaultBridge(Object originalPropertyValue) {
			this.originalPropertyValue = originalPropertyValue;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[originalPropertyValue=" + originalPropertyValue + "]";
		}

		@Override
		public boolean equals(Object o) {
			return getClass() == o.getClass()
					&& Objects.equals( originalPropertyValue,
							( (FieldTypeForOverridingDefaultBridge) o ).originalPropertyValue );
		}

		@Override
		public int hashCode() {
			return Objects.hash( originalPropertyValue );
		}
	}

	private static class OverridingDefaultBridge<V> implements ValueBridge<V, FieldTypeForOverridingDefaultBridge> {
		@Override
		public FieldTypeForOverridingDefaultBridge toIndexedValue(V value, ValueBridgeToIndexedValueContext context) {
			return new FieldTypeForOverridingDefaultBridge( value );
		}

		@SuppressWarnings("unchecked")
		@Override
		public V fromIndexedValue(FieldTypeForOverridingDefaultBridge value,
				ValueBridgeFromIndexedValueContext context) {
			return (V) value.originalPropertyValue;
		}

		@Override
		public FieldTypeForOverridingDefaultBridge parse(String value) {
			return new FieldTypeForOverridingDefaultBridge( "NULL_AS" );
		}
	}

}
