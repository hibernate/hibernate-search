/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContext;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.easymock.Capture;

/**
 * Test default identifier bridges for the {@code @DocumentId} annotation.
 */
public class DocumentIdDefaultBridgeIT {

	private static final String INDEX1_NAME = "Index1Name";
	private static final String INDEX2_NAME = "Index2Name";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void boxedInteger() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				id -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					return entity;
				},
				42,
				"42"
		);
	}

	@Test
	public void primitiveInteger() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			int id;
			@DocumentId
			public int getId() {
				return id;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			int id;
			@DocumentId
			public int getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				id -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					return entity;
				},
				42,
				"42"
		);
	}

	@Test
	public void boxedLong() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			Long id;
			@DocumentId
			public Long getId() {
				return id;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			Long id;
			@DocumentId
			public Long getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				id -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					return entity;
				},
				73L,
				"73"
		);
	}

	@Test
	public void primitiveLong() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			long id;
			@DocumentId
			public long getId() {
				return id;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			long id;
			@DocumentId
			public long getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				id -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					return entity;
				},
				9L,
				"9"
		);
	}

	@Test
	public void myEnum() {
		@Indexed(index = INDEX1_NAME)
		class IndexedEntity1 {
			MyEnum id;
			@DocumentId
			public MyEnum getId() {
				return id;
			}
		}
		@Indexed(index = INDEX2_NAME)
		class IndexedEntity2 {
			MyEnum id;
			@DocumentId
			public MyEnum getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity1.class,
				IndexedEntity2.class,
				id -> {
					IndexedEntity1 entity = new IndexedEntity1();
					entity.id = id;
					return entity;
				},
				MyEnum.VALUE1,
				"VALUE1"
		);
	}

	enum MyEnum {
		VALUE1,
		VALUE2
	}

	private <E, I> void doTestBridge(Class<E> entityType1, Class<?> entityType2,
			Function<I, E> newEntityFunction, I identifierValue, String identifierAsString) {
		// Schema
		Capture<StubIndexSchemaNode> schemaCapture1 = Capture.newInstance();
		Capture<StubIndexSchemaNode> schemaCapture2 = Capture.newInstance();
		backendMock.expectSchema( INDEX1_NAME, b -> { }, schemaCapture1 );
		backendMock.expectSchema( INDEX2_NAME, b -> { }, schemaCapture2 );
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( entityType1, entityType2 );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			E entity1 = newEntityFunction.apply( identifierValue );

			manager.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( identifierAsString, b -> { } )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			backendMock.expectSearchReferences(
					Collections.singletonList( INDEX1_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							StubBackendUtils.reference( INDEX1_NAME, identifierAsString )
					)
			);

			SearchQuery<PojoReference> query = manager.search( entityType1 )
					.query()
					.asReference()
					.predicate( f -> f.matchAll().toPredicate() )
					.build();

			assertThat( query )
					.hasHitsExactOrder( new PojoReferenceImpl( entityType1, identifierValue ) );
		}
		backendMock.verifyExpectationsMet();

		// DSL converter (to be used by the backend)
		StubIndexSchemaNode rootSchemaNode1 = schemaCapture1.getValue();
		StubIndexSchemaNode rootSchemaNode2 = schemaCapture2.getValue();
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		ToDocumentIdentifierValueConverter<I> dslToIndexConverter =
				(ToDocumentIdentifierValueConverter<I>) rootSchemaNode1.getIdDslConverter();
		ToDocumentIdentifierValueConverter<?> compatibleDslToIndexConverter =
				rootSchemaNode2.getIdDslConverter();
		ToDocumentIdentifierValueConvertContextImpl convertContext =
				new ToDocumentIdentifierValueConvertContextImpl( new JavaBeanMappingContext() );
		// isCompatibleWith must return true when appropriate
		Assertions.assertThat( dslToIndexConverter.isCompatibleWith( dslToIndexConverter ) ).isTrue();
		Assertions.assertThat( dslToIndexConverter.isCompatibleWith( compatibleDslToIndexConverter ) ).isTrue();
		Assertions.assertThat( dslToIndexConverter.isCompatibleWith( new IncompatibleToDocumentIdentifierValueConverter() ) )
				.isFalse();
		// convert and convertUnknown must behave appropriately on valid input
		Assertions.assertThat(
				dslToIndexConverter.convert( identifierValue, convertContext )
		)
				.isEqualTo( identifierAsString );
		Assertions.assertThat(
				dslToIndexConverter.convertUnknown( identifierValue, convertContext )
		)
				.isEqualTo( identifierAsString );
		// convertUnknown must throw a runtime exception on invalid input
		SubTest.expectException(
				"convertUnknown on invalid input",
				() -> dslToIndexConverter.convertUnknown( new Object(), convertContext )
		)
				.assertThrown()
				.isInstanceOf( RuntimeException.class );
	}

	private static class IncompatibleToDocumentIdentifierValueConverter
			implements ToDocumentIdentifierValueConverter<Object> {
		@Override
		public String convert(Object value, ToDocumentIdentifierValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String convertUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCompatibleWith(ToDocumentIdentifierValueConverter<?> other) {
			throw new UnsupportedOperationException();
		}
	}
}
