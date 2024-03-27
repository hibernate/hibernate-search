/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private final TestModelProvider testModelProvider;

	AbstractFieldContainerExtractorIT(TestModelProvider testModelProvider) {
		this.testModelProvider = testModelProvider;
	}

	@Test
	void objectArray() {
		doTest(
				testModelProvider.objectArray(),
				String.class, true,
				new String[] { STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 },
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void booleanArray() {
		doTest(
				testModelProvider.booleanArray(),
				Boolean.class, true,
				new boolean[] { true, false, true, true },
				true, false, true, true
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void charArray() {
		doTest(
				testModelProvider.charArray(),
				String.class, true, // The Character bridge maps them as Strings
				new char[] { 'a', 'b', 'c', 'd', '0', '\n', '*' },
				"a", "b", "c", "d", "0", "\n", "*"
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void byteArray() {
		doTest(
				testModelProvider.byteArray(),
				Byte.class, true,
				new byte[] { (byte) 0, (byte) 128, (byte) -15 },
				(byte) 0, (byte) 128, (byte) -15
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void shortArray() {
		doTest(
				testModelProvider.shortArray(),
				Short.class, true,
				new short[] { (short) 0, (short) 56782, (short) -15 },
				(short) 0, (short) 56782, (short) -15
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void intArray() {
		doTest(
				testModelProvider.intArray(),
				Integer.class, true,
				new int[] { 0, 14, Integer.MAX_VALUE, -55454, Integer.MIN_VALUE, 954466 },
				0, 14, Integer.MAX_VALUE, -55454, Integer.MIN_VALUE, 954466
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void longArray() {
		doTest(
				testModelProvider.longArray(),
				Long.class, true,
				new long[] { 0L, 14L, Long.MAX_VALUE, -55454L, Long.MIN_VALUE, 954466L },
				0L, 14L, Long.MAX_VALUE, -55454L, Long.MIN_VALUE, 954466L
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void floatArray() {
		doTest(
				testModelProvider.floatArray(),
				Float.class, true,
				new float[] { 0.0f, 14.48f, Float.MAX_VALUE, -55454.0f, Float.MIN_VALUE, -Float.MAX_VALUE, -Float.MIN_VALUE },
				0.0f, 14.48f, Float.MAX_VALUE, -55454.0f, Float.MIN_VALUE, -Float.MAX_VALUE, -Float.MIN_VALUE
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3997")
	void doubleArray() {
		doTest(
				testModelProvider.doubleArray(),
				Double.class, true,
				new double[] { 0.0, 14.48, Double.MAX_VALUE, -55454.0, Double.MIN_VALUE, -Double.MAX_VALUE, -Double.MIN_VALUE },
				0.0, 14.48, Double.MAX_VALUE, -55454.0, Double.MIN_VALUE, -Double.MAX_VALUE, -Double.MIN_VALUE
		);
	}

	@Test
	void iterable() {
		doTest(
				testModelProvider.iterable(),
				String.class, true,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	void collection() {
		doTest(
				testModelProvider.collection(),
				String.class, true,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	void list() {
		doTest(
				testModelProvider.list(),
				String.class, true,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	void set() {
		doTest(
				testModelProvider.set(),
				String.class, true,
				CollectionHelper.asLinkedHashSet( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2490")
	void sortedSet() {
		SortedSet<String> set = new TreeSet<>();
		// Do not add the strings in order, so as to really rely on the "sort" feature of the set
		Collections.addAll( set, STRING_VALUE_2, STRING_VALUE_1, STRING_VALUE_3 );
		doTest(
				testModelProvider.sortedSet(),
				String.class, true,
				set,
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	void mapValues() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put( STRING_VALUE_1, STRING_VALUE_4 );
		map.put( STRING_VALUE_2, STRING_VALUE_5 );
		map.put( STRING_VALUE_3, STRING_VALUE_6 );
		doTest(
				testModelProvider.mapValues(),
				String.class, true,
				map,
				STRING_VALUE_4, STRING_VALUE_5, STRING_VALUE_6
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2490")
	void sortedMapValues() {
		SortedMap<String, String> map = new TreeMap<>();
		// Do not add the strings in order, so as to really rely on the "sort" feature of the map
		map.put( STRING_VALUE_2, STRING_VALUE_5 );
		map.put( STRING_VALUE_1, STRING_VALUE_4 );
		map.put( STRING_VALUE_3, STRING_VALUE_6 );
		doTest(
				testModelProvider.sortedMapValues(),
				String.class, true,
				map,
				STRING_VALUE_4, STRING_VALUE_5, STRING_VALUE_6
		);
	}

	@Test
	void chain_mapListValues() {
		Map<String, List<String>> map = new LinkedHashMap<>();
		map.put( STRING_VALUE_1, CollectionHelper.asList( STRING_VALUE_2, STRING_VALUE_3 ) );
		map.put( STRING_VALUE_4, CollectionHelper.asList( STRING_VALUE_5, STRING_VALUE_6 ) );
		doTest(
				testModelProvider.mapListValues(),
				String.class, true,
				map,
				STRING_VALUE_2, STRING_VALUE_3, STRING_VALUE_5, STRING_VALUE_6
		);
	}

	@Test
	void optional_nonEmpty() {
		doTest(
				testModelProvider.optional(),
				String.class, false,
				Optional.of( STRING_VALUE_1 ),
				STRING_VALUE_1
		);
	}

	@Test
	void optional_empty() {
		doTestExpectMissing(
				testModelProvider.optional(),
				String.class, false,
				Optional.empty()
		);
	}

	@Test
	void optionalDouble_nonEmpty() {
		doTest(
				testModelProvider.optionalDouble(),
				Double.class, false,
				OptionalDouble.of( 42.42 ),
				42.42
		);
	}

	@Test
	void optionalDouble_empty() {
		doTestExpectMissing(
				testModelProvider.optionalDouble(),
				Double.class, false,
				OptionalDouble.empty()
		);
	}

	@Test
	void optionalInt_nonEmpty() {
		doTest(
				testModelProvider.optionalInt(),
				Integer.class, false,
				OptionalInt.of( 1 ),
				1
		);
	}

	@Test
	void optionalInt_empty() {
		doTestExpectMissing(
				testModelProvider.optionalInt(),
				Integer.class, false,
				OptionalInt.empty()
		);
	}

	@Test
	void optionalLong_nonEmpty() {
		doTest(
				testModelProvider.optionalLong(),
				Long.class, false,
				OptionalLong.of( 42L ),
				42L
		);
	}

	@Test
	void optionalLong_empty() {
		doTestExpectMissing(
				testModelProvider.optionalLong(),
				Long.class, false,
				OptionalLong.empty()
		);
	}

	/**
	 * Test that value bridges are actually applied to each element.
	 */
	@Test
	void list_nonPassThroughBridge() {
		doTest(
				testModelProvider.list_implicitEnumBridge(),
				String.class, true,
				CollectionHelper.asList( MyEnum.VALUE2, MyEnum.VALUE1 ),
				MyEnum.VALUE2.name(), MyEnum.VALUE1.name()
		);
	}

	/**
	 * Test that the user can pick a custom value bridge explicitly
	 * even when using a container value extractor.
	 */
	@Test
	void list_customBridge() {
		doTest(
				() -> setupHelper.start().expectCustomBeans(),
				testModelProvider.list_explicitPrefixedStringBridge(),
				String.class, true,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				PrefixedStringBridge.PREFIX + STRING_VALUE_1,
				PrefixedStringBridge.PREFIX + STRING_VALUE_2,
				PrefixedStringBridge.PREFIX + STRING_VALUE_3
		);
	}

	@SafeVarargs
	final <E, P, F> void doTest(Class<E> entityType,
			BiFunction<Integer, P, E> newEntityFunction,
			Class<F> indexedFieldType, boolean multiValued,
			P propertyValue, F firstIndexedFieldValues, F... otherIndexedFieldValues) {
		doTest(
				new TestModel<>( entityType, newEntityFunction ),
				indexedFieldType,
				multiValued,
				propertyValue, firstIndexedFieldValues, otherIndexedFieldValues
		);
	}

	@SafeVarargs
	final <E, P, F> void doTest(TestModel<E, P> testModel, Class<F> indexedFieldType, boolean multiValued,
			P propertyValue, F firstIndexedFieldValues, F... otherIndexedFieldValues) {
		doTest( setupHelper::start, testModel, indexedFieldType, multiValued,
				propertyValue, firstIndexedFieldValues, otherIndexedFieldValues );
	}

	@SafeVarargs
	final <E, P, F> void doTest(Supplier<StandalonePojoMappingSetupHelper.SetupContext> startSetup,
			TestModel<E, P> testModel, Class<F> indexedFieldType, boolean multiValued,
			P propertyValue, F firstIndexedFieldValues, F... otherIndexedFieldValues) {
		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", indexedFieldType, b2 -> {
					if ( multiValued ) {
						b2.multiValued( true );
					}
				} )
		);
		SearchMapping mapping = startSetup.get().setup( testModel.getEntityClass() );
		backendMock.verifyExpectationsMet();

		// Indexing with non-null, non-empty value
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = testModel.newEntity( 1, propertyValue );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field(
									"myProperty",
									firstIndexedFieldValues, (Object[]) otherIndexedFieldValues
							)
					);
		}
		backendMock.verifyExpectationsMet();

		// Indexing with null value
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = testModel.newEntity( 2, null );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "2", b -> {} );
		}
		backendMock.verifyExpectationsMet();
	}

	final <E, P, F> void doTestExpectMissing(TestModel<E, P> testModel,
			Class<F> indexedFieldType, boolean multiValued,
			P propertyValue) {
		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", indexedFieldType, b2 -> {
					if ( multiValued ) {
						b2.multiValued( true );
					}
				} )
		);
		SearchMapping mapping = setupHelper.start().setup( testModel.getEntityClass() );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = testModel.newEntity( 1, propertyValue );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> {} );
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
	}

	interface TestModelProvider {
		TestModel<?, String[]> objectArray();

		TestModel<?, char[]> charArray();

		TestModel<?, boolean[]> booleanArray();

		TestModel<?, byte[]> byteArray();

		TestModel<?, short[]> shortArray();

		TestModel<?, int[]> intArray();

		TestModel<?, long[]> longArray();

		TestModel<?, float[]> floatArray();

		TestModel<?, double[]> doubleArray();

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
