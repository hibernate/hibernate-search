/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test default value bridges for the {@code @GenericField} annotation.
 */
@RunWith(Parameterized.class)
public class FieldDefaultBridgeBaseIT<V, F> {
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
	private StubIndexNode index1Field;
	private StubIndexNode index2Field;

	public FieldDefaultBridgeBaseIT(PropertyTypeDescriptor<V, F> typeDescriptor, DefaultValueBridgeExpectations<V, F> expectations) {
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations;
	}

	@Before
	public void setup() {
		backendMock.expectSchema(
				DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME, b -> {
					b.field( FIELD_NAME, expectations.getIndexFieldJavaType() );

					if ( typeDescriptor.isNullable() ) {
						b.field( FIELD_INDEXNULLAS_NAME, expectations.getIndexFieldJavaType(), f -> f.indexNullAs( expectations.getNullAsValueBridge1() ) );
					}
				},
				indexModel -> this.index1Field = indexModel.fieldOrNull( FIELD_NAME )
		);
		backendMock.expectSchema(
				DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME, b -> {
					b.field( FIELD_NAME, expectations.getIndexFieldJavaType() );

					if ( typeDescriptor.isNullable() ) {
						b.field( FIELD_INDEXNULLAS_NAME, expectations.getIndexFieldJavaType(), f -> f.indexNullAs( expectations.getNullAsValueBridge2() ) );
					}
				},
				indexModel -> this.index2Field = indexModel.fieldOrNull( FIELD_NAME )
		);
		mapping = setupHelper.start()
				.withAnnotatedEntityType( expectations.getTypeWithValueBridge1(), DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME )
				.withAnnotatedEntityType( expectations.getTypeWithValueBridge2(), DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME )
				.setup();
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indexing() {
		try ( SearchSession session = mapping.createSession() ) {
			int id = 0;
			for ( V propertyValue : getPropertyValues() ) {
				Object entity = expectations.instantiateTypeWithValueBridge1( id, propertyValue );
				session.indexingPlan().add( entity );
				++id;
			}

			BackendMock.DocumentWorkCallListContext expectationSetter = backendMock.expectWorks(
					DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME
			);
			id = 0;
			for ( F expectedFieldValue : getDocumentFieldValues() ) {
				expectationSetter.add( String.valueOf( id ), b -> {
					b.field( FIELD_NAME, expectedFieldValue );

					if ( typeDescriptor.isNullable() ) {
						// Stub backend is not supposed to use 'indexNullAs' option
						b.field( FIELD_INDEXNULLAS_NAME, null );
					}
				} );
				++id;
			}
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
							2L,
							getDocumentFieldValues().toArray()
					)
			);

			assertThat( query.fetchAll().hits() )
					.containsExactlyElementsOf( getProjectionValues() );
		}
	}

	// Test behavior that backends expect from our bridges when using the DSLs
	@Test
	public void dslConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		DslConverter<V, ?> dslConverter =
				(DslConverter<V, ?>) index1Field.toValueField().type().dslConverter();
		DslConverter<?, ?> compatibleDslConverter =
				index2Field.toValueField().type().dslConverter();
		DslConverter<?, ?> incompatibleDslConverter =
				new DslConverter<>( typeDescriptor.getJavaType(), new IncompatibleToDocumentValueConverter<>() );
		ToDocumentValueConvertContext toDocumentConvertContext =
				new ToDocumentValueConvertContextImpl( BridgeTestUtils.toBackendMappingContext( mapping ) );

		// isCompatibleWith must return true when appropriate
		assertThat( dslConverter.isCompatibleWith( dslConverter ) ).isTrue();
		assertThat( dslConverter.isCompatibleWith( compatibleDslConverter ) ).isTrue();
		assertThat( dslConverter.isCompatibleWith( incompatibleDslConverter ) ).isFalse();

		// conversion methods must behave appropriately on valid input
		assertThat(
				dslConverter.toDocumentValue( null, toDocumentConvertContext )
		)
				.isNull();
		assertThat(
				dslConverter.unknownTypeToDocumentValue( null, toDocumentConvertContext )
		)
				.isNull();
		Iterator<F> fieldValuesIterator = getDocumentFieldValues().iterator();
		for ( V propertyValue : getPropertyValues() ) {
			F fieldValue = fieldValuesIterator.next();
			assertThat(
					dslConverter.toDocumentValue( propertyValue, toDocumentConvertContext )
			)
					.isEqualTo( fieldValue );
			assertThat(
					dslConverter.unknownTypeToDocumentValue( propertyValue, toDocumentConvertContext )
			)
					.isEqualTo( fieldValue );
		}

		// conversion methods must throw a runtime exception on invalid input
		assertThatThrownBy(
				() -> dslConverter.unknownTypeToDocumentValue( new Object(), toDocumentConvertContext ),
				"convertUnknown on invalid input"
		)
				.isInstanceOf( RuntimeException.class );
	}

	// Test behavior that backends expect from our bridges when using projections
	@Test
	public void projectionConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		ProjectionConverter<F, V> projectionConverter =
				(ProjectionConverter<F, V>) index1Field.toValueField().type().projectionConverter();
		ProjectionConverter<?, ?> compatibleProjectionConverter =
				index2Field.toValueField().type().projectionConverter();
		ProjectionConverter<?, ?> incompatibleProjectionConverter =
				new ProjectionConverter<>( typeDescriptor.getJavaType(), new IncompatibleFromDocumentValueConverter<>() );

		// isCompatibleWith must return true when appropriate
		assertThat( projectionConverter.isCompatibleWith( projectionConverter ) ).isTrue();
		assertThat( projectionConverter.isCompatibleWith( compatibleProjectionConverter ) )
				.isTrue();
		assertThat( projectionConverter.isCompatibleWith( incompatibleProjectionConverter ) ).isFalse();

		// withConvertedType must return the same converter for compatible types and throw an exception for clearly incompatible types
		assertThatCode( () -> projectionConverter.withConvertedType( Object.class,
				() -> EventContexts.fromIndexFieldAbsolutePath( "foo" ) ) )
				.doesNotThrowAnyException();
		assertThatCode( () -> projectionConverter.withConvertedType( typeDescriptor.getBoxedJavaType(),
				() -> EventContexts.fromIndexFieldAbsolutePath( "foo" ) ) )
				.doesNotThrowAnyException();
		assertThatThrownBy( () -> projectionConverter.withConvertedType( IncompatibleType.class,
				() -> EventContexts.fromIndexFieldAbsolutePath( "foo" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type for returned values: '" + IncompatibleType.class.getName() + "'",
						"Expected '" + typeDescriptor.getBoxedJavaType().getName() + "' or a supertype",
						"Context: field 'foo'"
				);

		// conversion methods must behave appropriately on valid input
		try ( SearchSession searchSession = mapping.createSession() ) {
			FromDocumentValueConvertContext fromDocumentConvertContext =
					new FromDocumentValueConvertContextImpl(
							BridgeTestUtils.toBackendSessionContext( searchSession )
					);
			assertThat(
					projectionConverter.fromDocumentValue( null, fromDocumentConvertContext )
			)
					.isNull();
			Iterator<V> projectionValuesIterator = getProjectionValues().iterator();
			for ( F fieldValue : getDocumentFieldValues() ) {
				V projectionValue = projectionValuesIterator.next();
				assertThat(
						projectionConverter.fromDocumentValue( fieldValue, fromDocumentConvertContext )
				)
						.isEqualTo( projectionValue );
			}
		}
	}

	private List<V> getPropertyValues() {
		List<V> propertyValues = new ArrayList<>( typeDescriptor.values().entityModelValues );
		if ( typeDescriptor.isNullable() ) {
			propertyValues.add( null );
		}
		return propertyValues;
	}

	private List<V> getProjectionValues() {
		List<V> values = typeDescriptor.values().entityModelValues.stream()
				.map( typeDescriptor::toProjectedValue )
				.collect( Collectors.toList() );
		if ( typeDescriptor.isNullable() ) {
			values.add( null );
		}
		return values;
	}

	private List<F> getDocumentFieldValues() {
		List<F> documentFieldValues = new ArrayList<>( typeDescriptor.values().documentFieldValues );
		if ( typeDescriptor.isNullable() ) {
			documentFieldValues.add( null );
		}
		return documentFieldValues;
	}

	/**
	 * A type that is clearly not a supertype of any type with a default bridge.
	 */
	private static final class IncompatibleType {
	}

	private static class IncompatibleToDocumentValueConverter<V>
			implements ToDocumentValueConverter<V, Object> {
		@Override
		public Object toDocumentValue(V value, ToDocumentValueConvertContext context) {
			throw new UnsupportedOperationException();
		}
	}

	private static class IncompatibleFromDocumentValueConverter<V>
			implements FromDocumentValueConverter<Object, V> {
		@Override
		public V fromDocumentValue(Object value, FromDocumentValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
			throw new UnsupportedOperationException();
		}
	}
}
