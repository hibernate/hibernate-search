/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.Month;
import java.util.function.BiFunction;

import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test default value bridges for the {@code @GenericField} annotation.
 */
public class FieldDefaultBridgeIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void string() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
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
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Integer.class,
				42
		);
	}

	@Test
	public void localDate() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				LocalDate.class,
				LocalDate.of( 2017, Month.NOVEMBER, 6 )
		);
	}

	@Test
	public void myEnum() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
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

	private <E, P> void doTestPassThroughBridge(Class<E> entityType,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<P> propertyAndIndexFieldType,
			P propertyAndIndexedFieldValue) {
		doTestBridge(
				entityType,
				newEntityFunction,
				propertyAndIndexFieldType, propertyAndIndexFieldType,
				propertyAndIndexedFieldValue, propertyAndIndexedFieldValue
		);
	}

	private <E, P, F> void doTestBridge(Class<E> entityType,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<P> propertyType, Class<F> indexedFieldType,
			P propertyValue, F indexedFieldValue) {
		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", indexedFieldType )
		);
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( entityType );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			E entity1 = newEntityFunction.apply( 1, propertyValue );

			manager.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field( "myProperty", indexedFieldValue )
					)
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		// TODO HSEARCH-3361 also test projections going through the bridge
	}

}
