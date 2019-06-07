/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.PropertyTypeDescriptor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.context.impl.JavaBeanSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.Assertions;
import org.easymock.Capture;

/**
 * Test default value bridges for the {@code @GenericField} annotation.
 */
@RunWith(Parameterized.class)
public class FieldDefaultBridgeIT<V, F> {
	private static final String FIELD_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_NAME;
	private static final String FIELD_INDEXNULLAS_NAME = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_FIELD_INDEXNULLAS_NAME;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] types() {
		return PropertyTypeDescriptor.getAll().stream()
				.map( type -> new Object[] { type, type.getDefaultValueBridgeExpectations() } )
				.toArray();
	}

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	private PropertyTypeDescriptor<V> typeDescriptor;
	private DefaultValueBridgeExpectations<V, F> expectations;
	private JavaBeanMapping mapping;
	private StubIndexSchemaNode index1FieldSchemaNode;
	private StubIndexSchemaNode index2FieldSchemaNode;

	public FieldDefaultBridgeIT(PropertyTypeDescriptor<V> typeDescriptor, Optional<DefaultValueBridgeExpectations<V, F>> expectations) {
		Assume.assumeTrue(
				"Type " + typeDescriptor + " does not have a default value bridge", expectations.isPresent()
		);
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations.get();
	}

	@Before
	public void setup() {
		Capture<StubIndexSchemaNode> schemaCapture1 = Capture.newInstance();
		Capture<StubIndexSchemaNode> schemaCapture2 = Capture.newInstance();
		backendMock.expectSchema(
				DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME, b -> {
					b.field( FIELD_NAME, expectations.getIndexFieldJavaType() );

					if ( typeDescriptor.isNullable() ) {
						b.field( FIELD_INDEXNULLAS_NAME, expectations.getIndexFieldJavaType(), f -> f.indexNullAs( expectations.getNullAsValueBridge1() ) );
					}
				}, schemaCapture1
		);
		backendMock.expectSchema(
				DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME, b -> {
					b.field( FIELD_NAME, expectations.getIndexFieldJavaType() );

					if ( typeDescriptor.isNullable() ) {
						b.field( FIELD_INDEXNULLAS_NAME, expectations.getIndexFieldJavaType(), f -> f.indexNullAs( expectations.getNullAsValueBridge2() ) );
					}
				}, schemaCapture2
		);
		mapping = setupHelper.withBackendMock( backendMock )
				.setup( expectations.getTypeWithValueBridge1(), expectations.getTypeWithValueBridge2() );
		backendMock.verifyExpectationsMet();
		index1FieldSchemaNode = schemaCapture1.getValue().getChildren().get( FIELD_NAME ).get( 0 );
		index2FieldSchemaNode = schemaCapture1.getValue().getChildren().get( FIELD_NAME ).get( 0 );
	}

	@Test
	public void indexing() {
		try ( SearchSession session = mapping.createSession() ) {
			int id = 0;
			for ( V propertyValue : getPropertyValues() ) {
				Object entity = expectations.instantiateTypeWithValueBridge1( id, propertyValue );
				session.getMainWorkPlan().add( entity );
				++id;
			}

			BackendMock.DocumentWorkCallListContext expectationSetter = backendMock.expectWorks(
					DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME
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
			expectationSetter.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void projection() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<V> query = session.search( expectations.getTypeWithValueBridge1() )
					.asProjection( f -> f.field( FIELD_NAME, expectations.getProjectionType() ) )
					.predicate( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchProjection(
					Collections
							.singletonList( DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME ),
					b -> {
					},
					StubSearchWorkBehavior.of(
							2L,
							getDocumentFieldValues().toArray()
					)
			);

			Assertions.<Object>assertThat( query.fetch().getHits() )
					.containsExactly( getPropertyValues().toArray() );
		}
	}

	// Test behavior that backends expect from our bridges when using the DSLs
	@Test
	public void dslToIndexConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		ToDocumentFieldValueConverter<V, ?> dslToIndexConverter =
				(ToDocumentFieldValueConverter<V, ?>) index1FieldSchemaNode.getConverter().getDslToIndexConverter();
		ToDocumentFieldValueConverter<?, ?> compatibleDslToIndexConverter =
				index2FieldSchemaNode.getConverter().getDslToIndexConverter();
		ToDocumentFieldValueConvertContext toDocumentConvertContext =
				new ToDocumentFieldValueConvertContextImpl( new JavaBeanMappingContext() );

		// isCompatibleWith must return true when appropriate
		assertThat( dslToIndexConverter.isCompatibleWith( dslToIndexConverter ) ).isTrue();
		assertThat( dslToIndexConverter.isCompatibleWith( compatibleDslToIndexConverter ) ).isTrue();
		assertThat(
				dslToIndexConverter.isCompatibleWith( new IncompatibleToDocumentFieldValueConverter() ) )
				.isFalse();

		// convert and convertUnknown must behave appropriately on valid input
		assertThat(
				dslToIndexConverter.convert( null, toDocumentConvertContext )
		)
				.isNull();
		assertThat(
				dslToIndexConverter.convertUnknown( null, toDocumentConvertContext )
		)
				.isNull();
		Iterator<F> fieldValuesIterator = getDocumentFieldValues().iterator();
		for ( V propertyValue : getPropertyValues() ) {
			F fieldValue = fieldValuesIterator.next();
			assertThat(
					dslToIndexConverter.convert( propertyValue, toDocumentConvertContext )
			)
					.isEqualTo( fieldValue );
			assertThat(
					dslToIndexConverter.convertUnknown( propertyValue, toDocumentConvertContext )
			)
					.isEqualTo( fieldValue );
		}

		// convertUnknown must throw a runtime exception on invalid input
		SubTest.expectException(
				"convertUnknown on invalid input",
				() -> dslToIndexConverter.convertUnknown( new Object(), toDocumentConvertContext )
		)
				.assertThrown()
				.isInstanceOf( RuntimeException.class );
	}

	// Test behavior that backends expect from our bridges when using projections
	@Test
	public void indexToProjectionConverter() {
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		FromDocumentFieldValueConverter<F, V> indexToProjectionConverter =
				(FromDocumentFieldValueConverter<F, V>) index1FieldSchemaNode.getConverter().getIndexToProjectionConverter();
		FromDocumentFieldValueConverter<?, ?> compatibleIndexToProjectionConverter =
				index2FieldSchemaNode.getConverter().getIndexToProjectionConverter();
		FromDocumentFieldValueConvertContext fromDocumentConvertContext =
				new FromDocumentFieldValueConvertContextImpl(
						new JavaBeanSessionContext(
								new JavaBeanMappingContext(),
								null,
								PojoRuntimeIntrospector.noProxy()
						)
				);

		// isCompatibleWith must return true when appropriate
		assertThat( indexToProjectionConverter.isCompatibleWith( indexToProjectionConverter ) ).isTrue();
		assertThat( indexToProjectionConverter.isCompatibleWith( compatibleIndexToProjectionConverter ) )
				.isTrue();
		assertThat( indexToProjectionConverter.isCompatibleWith( new IncompatibleFromDocumentFieldValueConverter() ) )
				.isFalse();

		// isConvertedTypeAssignableTo must return true for compatible types and false for clearly incompatible types
		assertThat( indexToProjectionConverter.isConvertedTypeAssignableTo( Object.class ) ).isTrue();
		assertThat( indexToProjectionConverter.isConvertedTypeAssignableTo( expectations.getProjectionType() ) ).isTrue();
		assertThat( indexToProjectionConverter.isConvertedTypeAssignableTo( IncompatibleType.class ) ).isFalse();

		// convert must behave appropriately on valid input
		assertThat(
				indexToProjectionConverter.convert( null, fromDocumentConvertContext )
		)
				.isNull();
		Iterator<V> propertyValuesIterator = getPropertyValues().iterator();
		for ( F fieldValue : getDocumentFieldValues() ) {
			V propertyValue = propertyValuesIterator.next();
			assertThat(
					indexToProjectionConverter.convert( fieldValue, fromDocumentConvertContext )
			)
					.isEqualTo( propertyValue );
		}
	}

	private List<V> getPropertyValues() {
		List<V> propertyValues = new ArrayList<>( expectations.getEntityPropertyValues() );
		if ( typeDescriptor.isNullable() ) {
			propertyValues.add( null );
		}
		return propertyValues;
	}

	private List<F> getDocumentFieldValues() {
		List<F> documentFieldValues = new ArrayList<>( expectations.getDocumentFieldValues() );
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

	private static class IncompatibleToDocumentFieldValueConverter
			implements ToDocumentFieldValueConverter<Object, Object> {
		@Override
		public Object convert(Object value, ToDocumentFieldValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object convertUnknown(Object value, ToDocumentFieldValueConvertContext context) {
			throw new UnsupportedOperationException();
		}
	}

	private static class IncompatibleFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<Object, Object> {
		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object convert(Object value, FromDocumentFieldValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			throw new UnsupportedOperationException();
		}
	}
}
