/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.search.JavaBeanSearchTarget;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;

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
	public void primitiveInteger() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
	public void boxedLong() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
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
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
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
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Boolean myProperty;
			@DocumentId
			public Integer getId() { return id; }
			@GenericField
			public Boolean getMyProperty() { return myProperty; }
		}
		doTestPassThroughBridge(
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
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
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			boolean myProperty;
			@DocumentId
			public Integer getId() { return id; }
			@GenericField
			public boolean getMyProperty() { return myProperty; }
		}
		doTestPassThroughBridge(
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
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
	public void utilDate() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
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
		doTestPassThroughBridge(
				IndexedEntity.class,
				(id, propertyValue) -> {
					IndexedEntity entity = new IndexedEntity();
					entity.id = id;
					entity.myProperty = propertyValue;
					return entity;
				},
				Date.class,
				new Date( 739739739L )
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
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
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
		try ( JavaBeanSearchManager manager = mapping.createSearchManager() ) {
			JavaBeanSearchTarget searchTarget = manager.search( entityType );
			SearchQuery<List<?>> query = searchTarget.query()
					.asProjections( searchTarget.projection().field( "myProperty" ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();

			backendMock.expectSearchProjections(
					Collections.singletonList( INDEX_NAME ),
					b -> {
					},
					StubSearchWorkBehavior.of(
							2L,
							Arrays.asList( indexedFieldValue ),
							Arrays.asList( indexedFieldValue )
					)
			);

			assertThat( query )
					.hasHitsExactOrder(
							Collections.singletonList( propertyValue ),
							Collections.singletonList( propertyValue )
					);
		}
	}

}
