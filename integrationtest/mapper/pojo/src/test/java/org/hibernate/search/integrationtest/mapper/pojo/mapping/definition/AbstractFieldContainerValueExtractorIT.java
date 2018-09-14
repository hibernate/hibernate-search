/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test default value bridges for the {@code @Field} annotation.
 */
public abstract class AbstractFieldContainerValueExtractorIT {

	static final String INDEX_NAME = "IndexName";

	static final String STRING_VALUE_1 = "1 - Some string";
	static final String STRING_VALUE_2 = "2 - Some other string";
	static final String STRING_VALUE_3 = "3 - Yet another string";
	static final String STRING_VALUE_4 = "4 - Still a different string";
	static final String STRING_VALUE_5 = "5 - Let's stop strings?";
	static final String STRING_VALUE_6 = "6 - The last string";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	private final TestModelProvider testModelProvider;

	AbstractFieldContainerValueExtractorIT(TestModelProvider testModelProvider) {
		this.testModelProvider = testModelProvider;
	}

	@Test
	public void objectArray() {
		doTest(
				testModelProvider.objectArray(),
				String.class,
				new String[] { STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 },
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	public void iterable() {
		doTest(
				testModelProvider.iterable(),
				String.class,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	public void collection() {
		doTest(
				testModelProvider.collection(),
				String.class,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	public void list() {
		doTest(
				testModelProvider.list(),
				String.class,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	public void set() {
		doTest(
				testModelProvider.set(),
				String.class,
				CollectionHelper.asLinkedHashSet( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2490")
	public void sortedSet() {
		SortedSet<String> set = new TreeSet<>();
		// Do not add the strings in order, so as to really rely on the "sort" feature of the set
		Collections.addAll( set, STRING_VALUE_2, STRING_VALUE_1, STRING_VALUE_3 );
		doTest(
				testModelProvider.sortedSet(),
				String.class,
				set,
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	public void mapValues() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put( STRING_VALUE_1, STRING_VALUE_4 );
		map.put( STRING_VALUE_2, STRING_VALUE_5 );
		map.put( STRING_VALUE_3, STRING_VALUE_6 );
		doTest(
				testModelProvider.mapValues(),
				String.class,
				map,
				STRING_VALUE_4, STRING_VALUE_5, STRING_VALUE_6
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2490")
	public void sortedMapValues() {
		SortedMap<String, String> map = new TreeMap<>();
		// Do not add the strings in order, so as to really rely on the "sort" feature of the map
		map.put( STRING_VALUE_2, STRING_VALUE_5 );
		map.put( STRING_VALUE_1, STRING_VALUE_4 );
		map.put( STRING_VALUE_3, STRING_VALUE_6 );
		doTest(
				testModelProvider.sortedMapValues(),
				String.class,
				map,
				STRING_VALUE_4, STRING_VALUE_5, STRING_VALUE_6
		);
	}

	@Test
	public void chain_mapListValues() {
		Map<String, List<String>> map = new LinkedHashMap<>();
		map.put( STRING_VALUE_1, CollectionHelper.asList( STRING_VALUE_2, STRING_VALUE_3 ) );
		map.put( STRING_VALUE_4, CollectionHelper.asList( STRING_VALUE_5, STRING_VALUE_6 ) );
		doTest(
				testModelProvider.mapListValues(),
				String.class,
				map,
				STRING_VALUE_2, STRING_VALUE_3, STRING_VALUE_5, STRING_VALUE_6
		);
	}

	@Test
	public void optional_nonEmpty() {
		doTest(
				testModelProvider.optional(),
				String.class,
				Optional.of( STRING_VALUE_1 ),
				STRING_VALUE_1
		);
	}

	@Test
	public void optional_empty() {
		doTestExpectMissing(
				testModelProvider.optional(),
				String.class,
				Optional.empty()
		);
	}

	@Test
	public void optionalInt_nonEmpty() {
		doTest(
				testModelProvider.optionalInt(),
				Integer.class,
				OptionalInt.of( 1 ),
				1
		);
	}

	@Test
	public void optionalInt_empty() {
		doTestExpectMissing(
				testModelProvider.optionalInt(),
				Integer.class,
				OptionalInt.empty()
		);
	}

	/**
	 * Test that value bridges are actually applied to each element.
	 */
	@Test
	public void list_nonPassThroughBridge() {
		doTest(
				testModelProvider.list_implicitEnumBridge(),
				String.class,
				CollectionHelper.asList( MyEnum.VALUE2, MyEnum.VALUE1 ),
				MyEnum.VALUE2.name(), MyEnum.VALUE1.name()
		);
	}

	/**
	 * Test that the user can pick a custom value bridge explicitly
	 * even when using a container value extractor.
	 */
	@Test
	public void list_customBridge() {
		doTest(
				testModelProvider.list_explicitPrefixedStringBridge(),
				String.class,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				PrefixedStringBridge.PREFIX + STRING_VALUE_1,
				PrefixedStringBridge.PREFIX + STRING_VALUE_2,
				PrefixedStringBridge.PREFIX + STRING_VALUE_3
		);
	}

	@SafeVarargs
	final <E, P, F> void doTest(Class<E> entityType,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<F> indexedFieldType,
			P propertyValue, F firstIndexedFieldValues, F... otherIndexedFieldValues) {
		doTest(
				new TestModel<>( entityType, newEntityFunction ),
				indexedFieldType,
				propertyValue, firstIndexedFieldValues, otherIndexedFieldValues
		);
	}

	@SafeVarargs
	final <E, P, F> void doTest(TestModel<E, P> testModel, Class<F> indexedFieldType,
			P propertyValue, F firstIndexedFieldValues, F... otherIndexedFieldValues) {
		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", indexedFieldType )
		);
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( testModel.getEntityClass() );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			E entity1 = testModel.newEntity( 1, propertyValue );

			manager.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field(
									"myProperty",
									firstIndexedFieldValues, (Object[]) otherIndexedFieldValues
							)
					)
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		// TODO HSEARCH-3361 + HSEARCH-1895 also test projections going through the extractor
	}

	final <E, P, F> void doTestExpectMissing(TestModel<E, P> testModel, Class<F> indexedFieldType,
			P propertyValue) {
		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", indexedFieldType )
		);
		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock ).setup( testModel.getEntityClass() );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			E entity1 = testModel.newEntity( 1, propertyValue );

			manager.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();

		// Searching
		// TODO HSEARCH-3361 + HSEARCH-1895 also test projections going through the extractor
	}

	public enum MyEnum {
		VALUE1,
		VALUE2
	}

	public static class PrefixedStringBridge implements ValueBridge<String, String> {
		public static final String PREFIX = "Prefix - ";
		@Override
		public String toIndexedValue(String value) {
			return value == null ? null : PREFIX + value;
		}
		@Override
		public String cast(Object value) {
			return (String) value;
		}
	}

	interface TestModelProvider {
		TestModel<?, String[]> objectArray();
		TestModel<?, Iterable<String>> iterable();
		TestModel<?, Collection<String>> collection();
		TestModel<?, List<String>> list();
		TestModel<?, Set<String>> set();
		TestModel<?, SortedSet<String>> sortedSet();
		TestModel<?, Map<String, String>> mapValues();
		TestModel<?, SortedMap<String, String>> sortedMapValues();
		TestModel<?, Map<String, List<String>>> mapListValues();
		TestModel<?, Optional<String>> optional();
		TestModel<?, OptionalInt> optionalInt();
		TestModel<?, List<String>> list_explicitPrefixedStringBridge();
		TestModel<?, List<MyEnum>> list_implicitEnumBridge();
	}

	static class TestModel<E, P> {
		private final Class<E> entityClass;
		private final BiFunction<Integer, P, E> entityConstructor;

		TestModel(Class<E> entityClass, BiFunction<Integer, P, E> entityConstructor) {
			this.entityClass = entityClass;
			this.entityConstructor = entityConstructor;
		}

		final Class<E> getEntityClass() {
			return entityClass;
		}

		final E newEntity(int id, P propertyValue) {
			return entityConstructor.apply( id, propertyValue );
		}
	}

}
