/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test overriding default value bridges for the {@code @GenericField} annotation,
 * for example assigning a different default value bridge for properties of type {@link String}.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3096")
public class FieldDefaultBridgeOverridingIT<V, F> {
	private static final String FIELD_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_NAME;
	private static final String FIELD_INDEXNULLAS_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_INDEXNULLAS_NAME;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] types() {
		return PropertyTypeDescriptor.getAll().stream()
				.map( type -> new Object[] { type, type.getDefaultValueBridgeExpectations() } )
				.toArray();
	}

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private final PropertyTypeDescriptor<V, F> typeDescriptor;
	private final DefaultValueBridgeExpectations<V, F> expectations;
	private SearchMapping mapping;
	private StubIndexNode indexField;

	public FieldDefaultBridgeOverridingIT(PropertyTypeDescriptor<V, F> typeDescriptor, DefaultValueBridgeExpectations<V, F> expectations) {
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations;
	}

	@Before
	public void setup() {
		backendMock.expectSchema(
				DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME, b -> {
					b.field( FIELD_NAME, FieldTypeForOverridingDefaultBridge.class );

					if ( typeDescriptor.isNullable() ) {
						b.field( FIELD_INDEXNULLAS_NAME, FieldTypeForOverridingDefaultBridge.class,
								f -> f.indexNullAs( new FieldTypeForOverridingDefaultBridge( "NULL_AS" ) ) );
					}
				},
				indexModel -> this.indexField = indexModel.fieldOrNull( FIELD_NAME )
		);
		mapping = setupHelper.start()
				.withAnnotatedEntityType( expectations.getTypeWithValueBridge1(),
						DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME )
				// HERE we override the default bridge for the type being tested.
				.withConfiguration( builder -> builder.bridges().exactType( typeDescriptor.getJavaType() )
						.valueBridge( new OverridingDefaultBridge<>() ) )
				.setup();
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indexing() {
		try ( SearchSession session = mapping.createSession() ) {
			Object entity = expectations.instantiateTypeWithValueBridge1( 1, getPropertyValue() );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME )
					.add( "1", b -> {
						b.field( FIELD_NAME, getFieldValue() );

						if ( typeDescriptor.isNullable() ) {
							// The stub backend does not implement the 'indexNullAs' option
							b.field( FIELD_INDEXNULLAS_NAME, new FieldTypeForOverridingDefaultBridge( null ) );
						}
					} );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void projection() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<V> query = session.search( expectations.getTypeWithValueBridge1() )
					.select( f -> f.field( FIELD_NAME, typeDescriptor.getBoxedJavaType() ) )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME,
					StubSearchWorkBehavior.of(
							1L,
							getFieldValue()
					)
			);

			assertThat( query.fetchAll().hits() ).containsExactly( getPropertyValue() );
		}
	}

	@Test
	public void dslConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		DslConverter<V, ?> dslConverter =
				(DslConverter<V, ?>) indexField.toValueField().type().dslConverter();
		ToDocumentValueConvertContext toDocumentConvertContext =
				new ToDocumentValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );

		// The overriden default bridge must be used by the DSL converter
		assertThat( dslConverter.toDocumentValue( getPropertyValue(), toDocumentConvertContext ) )
				.isEqualTo( getFieldValue() );
		assertThat( dslConverter.unknownTypeToDocumentValue( getPropertyValue(), toDocumentConvertContext ) )
				.isEqualTo( getFieldValue() );
	}

	private V getPropertyValue() {
		return typeDescriptor.values().entityModelValues.get( 0 );
	}

	private FieldTypeForOverridingDefaultBridge getFieldValue() {
		return new FieldTypeForOverridingDefaultBridge( getPropertyValue() );
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
			return getClass() == o.getClass() && Objects.equals( originalPropertyValue,
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
