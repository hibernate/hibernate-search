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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test default value bridges for the {@code @GenericField} annotation.
 */
public abstract class AbstractFieldContainerExtractorIT {

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

	AbstractFieldContainerExtractorIT(TestModelProvider testModelProvider) {
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
	@Ignore // TODO HSEARCH-3047 enable this test when we add support for the Double type
	public void optionalDouble_nonEmpty() {
		doTest(
				testModelProvider.optionalDouble(),
				Double.class,
				OptionalDouble.of( 42.42 ),
				42.42
		);
	}

	@Test
	@Ignore // TODO HSEARCH-3047 enable this test when we add support for the Double type
	public void optionalDouble_empty() {
		doTestExpectMissing(
				testModelProvider.optionalDouble(),
				Double.class,
				OptionalDouble.empty()
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

	@Test
	public void optionalLong_nonEmpty() {
		doTest(
				testModelProvider.optionalLong(),
				Long.class,
				OptionalLong.of( 42L ),
				42L
		);
	}

	@Test
	public void optionalLong_empty() {
		doTestExpectMissing(
				testModelProvider.optionalLong(),
				Long.class,
				OptionalLong.empty()
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
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = testModel.newEntity( 1, propertyValue );

			session.getMainWorkPlan().add( entity1 );

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
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = testModel.newEntity( 1, propertyValue );

			session.getMainWorkPlan().add( entity1 );

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
		public String toIndexedValue(String value,
				ValueBridgeToIndexedValueContext context) {
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
		TestModel<?, OptionalDouble> optionalDouble();
		TestModel<?, OptionalInt> optionalInt();
		TestModel<?, OptionalLong> optionalLong();
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
