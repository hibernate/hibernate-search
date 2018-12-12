/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.Date;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContext;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.javabean.session.context.impl.JavaBeanSessionContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.easymock.Capture;

/**
 * Test default value bridges for the {@code @GenericField} annotation.
 */
public class FieldDefaultBridgeIT {

	private static final String INDEX1_NAME = "Index1Name";
	private static final String INDEX2_NAME = "Index2Name";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void string() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			String myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			String myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public String getMyProperty() {
				return myProperty;
			}
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				String.class,
				"some string"
		);
	}

	@Test
	public void boxedInteger() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			Integer myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Integer getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			Integer myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Integer getMyProperty() {
				return myProperty;
			}
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Integer.class,
				42
		);
	}

	@Test
	public void primitiveInteger() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			int myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public int getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			int myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public int getMyProperty() {
				return myProperty;
			}
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Integer.class,
				42
		);
	}

	@Test
	public void boxedLong() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			Long myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Long getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			Long myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Long getMyProperty() {
				return myProperty;
			}
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Long.class,
				39L
		);
	}

	@Test
	public void primitiveLong() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			long myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public long getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			long myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public long getMyProperty() {
				return myProperty;
			}
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Long.class,
				7L
		);
	}

	@Test
	public void boxedBoolean() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			Boolean myProperty;
			@DocumentId
			public Integer getId() { return id; }
			@GenericField
			public Boolean getMyProperty() { return myProperty; }
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			Boolean myProperty;
			@DocumentId
			public Integer getId() { return id; }
			@GenericField
			public Boolean getMyProperty() { return myProperty; }
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Boolean.class,
				true
		);
	}

	@Test
	public void primitiveBoolean() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			boolean myProperty;
			@DocumentId
			public Integer getId() { return id; }
			@GenericField
			public boolean getMyProperty() { return myProperty; }
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			boolean myProperty;
			@DocumentId
			public Integer getId() { return id; }
			@GenericField
			public boolean getMyProperty() { return myProperty; }
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Boolean.class,
				true
		);
	}

	@Test
	public void localDate() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			LocalDate myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public LocalDate getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			LocalDate myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public LocalDate getMyProperty() {
				return myProperty;
			}
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				LocalDate.class,
				LocalDate.of( 2017, Month.NOVEMBER, 6 )
		);
	}

	@Test
	public void instant() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			Instant myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Instant getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			Instant myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Instant getMyProperty() {
				return myProperty;
			}
		}
		doTestPassThroughBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Instant.class,
				Instant.parse( "1970-01-09T13:28:59.00Z" )
		);
	}

	@Test
	public void javaUtilDate() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			Date myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Date getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			Date myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public Date getMyProperty() {
				return myProperty;
			}
		}
		Instant instant = Instant.parse( "1970-01-09T13:28:59.00Z" );
		doTestBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Date.class, Instant.class,
				Date.from( instant ), instant
		);
	}

	@Test
	public void myEnum() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			MyEnum myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public MyEnum getMyProperty() {
				return myProperty;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			MyEnum myProperty;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField
			public MyEnum getMyProperty() {
				return myProperty;
			}
		}
		doTestBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				(id, propertyValue) -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				MyEnum.class, String.class,
				MyEnum.VALUE1, "VALUE1"
		);
	}

	enum MyEnum {
		VALUE1,
		VALUE2
	}

	private <E, P> void doTestPassThroughBridge(Class<E> entityType1, Class<?> entityType2,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<P> propertyAndIndexFieldType,
			P propertyAndIndexedFieldValue) {
		doTestBridge(
				entityType1, entityType2,
				newEntityFunction,
				propertyAndIndexFieldType, propertyAndIndexFieldType,
				propertyAndIndexedFieldValue, propertyAndIndexedFieldValue
		);
	}

	private <E, P, F> void doTestBridge(Class<E> entityType1, Class<?> entityType2,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<P> propertyType, Class<F> indexedFieldType,
			P propertyValue, F indexedFieldValue) {
		// Schema
		Capture<StubIndexSchemaNode> schemaCapture1 = Capture.newInstance();
		Capture<StubIndexSchemaNode> schemaCapture2 = Capture.newInstance();
		backendMock.expectSchema(
				INDEX1_NAME,
				b -> b.field( "myProperty", indexedFieldType ),
				schemaCapture1
		);
		backendMock.expectSchema(
				INDEX2_NAME,
				b -> b.field( "myProperty", indexedFieldType ),
				schemaCapture2
		);
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( entityType1, entityType2 );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			E entity1 = newEntityFunction.apply( 1, propertyValue );

			manager.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "myProperty", indexedFieldValue )
					)
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			SearchQuery<P> query = manager.search( entityType1 ).query()
					.asProjection( f -> f.field( "myProperty", propertyType ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();

			backendMock.expectSearchProjection(
					Collections.singletonList( INDEX1_NAME ),
					b -> {
					},
					StubSearchWorkBehavior.of(
							2L,
							indexedFieldValue,
							indexedFieldValue
					)
			);

			assertThat( query )
					.hasHitsExactOrder(
							propertyValue,
							propertyValue
					);
		}

		// DSL converter (to be used by the backend)
		StubIndexSchemaNode index1FieldSchemaNode = schemaCapture1.getValue().getChildren().get( "myProperty" ).get( 0 );
		StubIndexSchemaNode index2FieldSchemaNode = schemaCapture1.getValue().getChildren().get( "myProperty" ).get( 0 );
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		ToDocumentFieldValueConverter<P, ?> dslToIndexConverter =
				(ToDocumentFieldValueConverter<P, ?>) index1FieldSchemaNode.getConverter().getDslToIndexConverter();
		ToDocumentFieldValueConverter<?, ?> compatibleDslToIndexConverter =
				index2FieldSchemaNode.getConverter().getDslToIndexConverter();
		ToDocumentFieldValueConvertContext toDocumentConvertContext =
				new ToDocumentFieldValueConvertContextImpl( new JavaBeanMappingContext() );
		// isCompatibleWith must return true when appropriate
		Assertions.assertThat( dslToIndexConverter.isCompatibleWith( dslToIndexConverter ) ).isTrue();
		Assertions.assertThat( dslToIndexConverter.isCompatibleWith( compatibleDslToIndexConverter ) ).isTrue();
		Assertions.assertThat( dslToIndexConverter.isCompatibleWith( new IncompatibleToDocumentFieldValueConverter() ) )
				.isFalse();
		// convert and convertUnknown must behave appropriately on valid input
		Assertions.assertThat(
				dslToIndexConverter.convert( null, toDocumentConvertContext )
		)
				.isNull();
		Assertions.assertThat(
				dslToIndexConverter.convertUnknown( null, toDocumentConvertContext )
		)
				.isNull();
		Assertions.assertThat(
				dslToIndexConverter.convert( propertyValue, toDocumentConvertContext )
		)
				.isEqualTo( indexedFieldValue );
		Assertions.assertThat(
				dslToIndexConverter.convertUnknown( propertyValue, toDocumentConvertContext )
		)
				.isEqualTo( indexedFieldValue );
		// convertUnknown must throw a runtime exception on invalid input
		SubTest.expectException(
				"convertUnknown on invalid input",
				() -> dslToIndexConverter.convertUnknown( new Object(), toDocumentConvertContext )
		)
				.assertThrown()
				.isInstanceOf( RuntimeException.class );

		// Projection converter (to be used by the backend)
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		FromDocumentFieldValueConverter<F, P> indexToProjectionConverter =
				(FromDocumentFieldValueConverter<F, P>) index1FieldSchemaNode.getConverter().getIndexToProjectionConverter();
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
		Assertions.assertThat( indexToProjectionConverter.isCompatibleWith( indexToProjectionConverter ) ).isTrue();
		Assertions.assertThat( indexToProjectionConverter.isCompatibleWith( compatibleIndexToProjectionConverter ) )
				.isTrue();
		Assertions.assertThat( indexToProjectionConverter.isCompatibleWith( new IncompatibleFromDocumentFieldValueConverter() ) )
				.isFalse();
		// isConvertedTypeAssignableTo must return true for compatible types and false for clearly incompatible types
		Assertions.assertThat( indexToProjectionConverter.isConvertedTypeAssignableTo( Object.class ) ).isTrue();
		Assertions.assertThat( indexToProjectionConverter.isConvertedTypeAssignableTo( propertyType ) ).isTrue();
		Assertions.assertThat( indexToProjectionConverter.isConvertedTypeAssignableTo( IncompatibleType.class ) ).isFalse();
		// convert must behave appropriately on valid input
		Assertions.assertThat(
				indexToProjectionConverter.convert( null, fromDocumentConvertContext )
		)
				.isNull();
		Assertions.assertThat(
				indexToProjectionConverter.convert( indexedFieldValue, fromDocumentConvertContext )
		)
				.isEqualTo( propertyValue );
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
