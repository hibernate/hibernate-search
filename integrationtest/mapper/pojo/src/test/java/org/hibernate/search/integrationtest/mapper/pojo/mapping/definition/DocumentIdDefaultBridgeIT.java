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

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void boxedInteger() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity.class,
				id -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					return entity;
				},
				42,
				"42"
		);
	}

	@Test
	public void primitiveInteger() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			int id;
			@DocumentId
			public int getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity.class,
				id -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					return entity;
				},
				42,
				"42"
		);
	}

	@Test
	public void boxedLong() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Long id;
			@DocumentId
			public Long getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity.class,
				id -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					return entity;
				},
				73L,
				"73"
		);
	}

	@Test
	public void primitiveLong() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			long id;
			@DocumentId
			public long getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity.class,
				id -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					return entity;
				},
				9L,
				"9"
		);
	}

	@Test
	public void myEnum() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			MyEnum id;
			@DocumentId
			public MyEnum getId() {
				return id;
			}
		}
		doTestBridge(
				IndexedEntity.class,
				id -> {
					IndexedEntity entity = new IndexedEntity();
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

	private <E, I> void doTestBridge(Class<E> entityType,
			Function<I, E> newEntityFunction, I identifierValue, String identifierAsString) {
		// Schema
		Capture<StubIndexSchemaNode> schemaCapture = Capture.newInstance();
		backendMock.expectSchema( INDEX_NAME, b -> { }, schemaCapture );
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( entityType );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			E entity1 = newEntityFunction.apply( identifierValue );

			manager.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( identifierAsString, b -> { } )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			backendMock.expectSearchReferences(
					Collections.singletonList( INDEX_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							StubBackendUtils.reference( INDEX_NAME, identifierAsString )
					)
			);

			SearchQuery<PojoReference> query = manager.search( entityType )
					.query()
					.asReference()
					.predicate( f -> f.matchAll().toPredicate() )
					.build();

			assertThat( query )
					.hasHitsExactOrder( new PojoReferenceImpl( entityType, identifierValue ) );
		}
		backendMock.verifyExpectationsMet();

		// DSL converter (to be used by the backend)
		StubIndexSchemaNode rootSchemaNode = schemaCapture.getValue();
		// This cast may be unsafe, but only if something is deeply wrong, and then an exception will be thrown below
		@SuppressWarnings("unchecked")
		ToDocumentIdentifierValueConverter<I> dslToIndexConverter =
				(ToDocumentIdentifierValueConverter<I>) rootSchemaNode.getIdDslConverter();
		ToDocumentIdentifierValueConvertContextImpl convertContext =
				new ToDocumentIdentifierValueConvertContextImpl( new JavaBeanMappingContext() );
		// isCompatibleWith must return true when appropriate
		Assertions.assertThat( dslToIndexConverter.isCompatibleWith( dslToIndexConverter ) ).isTrue();
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

}
