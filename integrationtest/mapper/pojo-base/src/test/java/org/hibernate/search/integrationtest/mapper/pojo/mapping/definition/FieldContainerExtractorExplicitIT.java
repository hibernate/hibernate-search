/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.util.Collection;
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

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Test explicit container value extractors for the {@code @GenericField} annotation.
 * <p>
 * Error cases are not tested here but in {@link FieldContainerExtractorBaseIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-2554")
public class FieldContainerExtractorExplicitIT extends AbstractFieldContainerExtractorIT {

	public FieldContainerExtractorExplicitIT() {
		super( new ExplicitContainerExtractorTestModelProvider() );
	}

	@Test
	public void mapKeys() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			private Integer id;
			@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY))
			private Map<String, String> myProperty;

			IndexedEntity(int id, Map<String, String> myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}
		}
		Map<String, String> map = new LinkedHashMap<>();
		map.put( STRING_VALUE_1, STRING_VALUE_4 );
		map.put( STRING_VALUE_2, STRING_VALUE_5 );
		map.put( STRING_VALUE_3, STRING_VALUE_6 );
		doTest(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				String.class, true,
				map,
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	public void chain_mapListKeys() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			private Integer id;
			@GenericField(extraction = @ContainerExtraction({
					BuiltinContainerExtractors.MAP_KEY,
					BuiltinContainerExtractors.ITERABLE
			}))
			private Map<List<String>, String> myProperty;

			private IndexedEntity(int id, Map<List<String>, String> myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}
		}
		Map<List<String>, String> map = new LinkedHashMap<>();
		map.put( CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2 ), STRING_VALUE_3 );
		map.put( CollectionHelper.asList( STRING_VALUE_4, STRING_VALUE_5 ), STRING_VALUE_6 );
		doTest(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				String.class, true,
				map,
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_4, STRING_VALUE_5
		);
	}

	@Test
	public void containerBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			private Integer id;
			@GenericField(
					extraction = @ContainerExtraction(extract = ContainerExtract.NO),
					valueBridge = @ValueBridgeRef(type = FirstCollectionElementBridge.class)
			)
			private List<String> myProperty;

			private IndexedEntity(int id, List<String> myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}
		}

		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "myProperty", String.class )
		);
		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		// Indexing
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity( 1,
					CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ) );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "myProperty", STRING_VALUE_1 ) );
		}
		backendMock.verifyExpectationsMet();
	}

	private static final class ExplicitContainerExtractorTestModelProvider implements TestModelProvider {

		@Override
		public TestModel<?, String[]> objectArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_OBJECT))
				private String[] myProperty;

				private IndexedEntity(int id, String[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, char[]> charArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_CHAR))
				private char[] myProperty;

				private IndexedEntity(int id, char[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, boolean[]> booleanArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_BOOLEAN))
				private boolean[] myProperty;

				private IndexedEntity(int id, boolean[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, byte[]> byteArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_BYTE))
				private byte[] myProperty;

				private IndexedEntity(int id, byte[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, short[]> shortArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_SHORT))
				private short[] myProperty;

				private IndexedEntity(int id, short[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, int[]> intArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_INT))
				private int[] myProperty;

				private IndexedEntity(int id, int[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, long[]> longArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_LONG))
				private long[] myProperty;

				private IndexedEntity(int id, long[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, float[]> floatArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_FLOAT))
				private float[] myProperty;

				private IndexedEntity(int id, float[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, double[]> doubleArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ARRAY_DOUBLE))
				private double[] myProperty;

				private IndexedEntity(int id, double[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Iterable<String>> iterable() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ITERABLE))
				private Iterable<String> myProperty;

				private IndexedEntity(int id, Iterable<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Collection<String>> collection() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ITERABLE))
				private Collection<String> myProperty;

				private IndexedEntity(int id, Collection<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, List<String>> list() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ITERABLE))
				private List<String> myProperty;

				private IndexedEntity(int id, List<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Set<String>> set() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ITERABLE))
				private Set<String> myProperty;

				private IndexedEntity(int id, Set<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, SortedSet<String>> sortedSet() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ITERABLE))
				private SortedSet<String> myProperty;

				private IndexedEntity(int id, SortedSet<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Map<String, String>> mapValues() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
				private Map<String, String> myProperty;

				private IndexedEntity(int id, Map<String, String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, SortedMap<String, String>> sortedMapValues() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
				private SortedMap<String, String> myProperty;

				private IndexedEntity(int id, SortedMap<String, String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Map<String, List<String>>> mapListValues() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction({
						BuiltinContainerExtractors.MAP_VALUE,
						BuiltinContainerExtractors.ITERABLE
				}))
				private Map<String, List<String>> myProperty;

				private IndexedEntity(int id, Map<String, List<String>> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Optional<String>> optional() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.OPTIONAL))
				private Optional<String> myProperty;

				private IndexedEntity(int id, Optional<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, OptionalDouble> optionalDouble() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.OPTIONAL_DOUBLE))
				private OptionalDouble myProperty;

				private IndexedEntity(int id, OptionalDouble myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, OptionalInt> optionalInt() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.OPTIONAL_INT))
				private OptionalInt myProperty;

				private IndexedEntity(int id, OptionalInt myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, OptionalLong> optionalLong() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.OPTIONAL_LONG))
				private OptionalLong myProperty;

				private IndexedEntity(int id, OptionalLong myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, List<String>> list_explicitPrefixedStringBridge() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(
						valueBridge = @ValueBridgeRef(type = PrefixedStringBridge.class),
						extraction = @ContainerExtraction(BuiltinContainerExtractors.ITERABLE)
				)
				private List<String> myProperty;

				private IndexedEntity(int id, List<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, List<MyEnum>> list_implicitEnumBridge() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.ITERABLE))
				private List<MyEnum> myProperty;

				private IndexedEntity(int id, List<MyEnum> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}
	}

	@SuppressWarnings("rawtypes")
	public static class FirstCollectionElementBridge implements ValueBridge<Collection, String> {
		@Override
		public String toIndexedValue(Collection value,
				ValueBridgeToIndexedValueContext context) {
			return value == null || value.isEmpty() ? null : (String) value.iterator().next();
		}
	}

}
