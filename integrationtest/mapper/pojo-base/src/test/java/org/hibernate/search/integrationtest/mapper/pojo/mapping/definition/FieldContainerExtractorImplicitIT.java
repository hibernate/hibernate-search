/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

/**
 * Test implicit container value extractors for the {@code @GenericField} annotation.
 * <p>
 * Error cases are not tested here but in {@link FieldContainerExtractorBaseIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-2554")
class FieldContainerExtractorImplicitIT extends AbstractFieldContainerExtractorIT {

	public FieldContainerExtractorImplicitIT() {
		super( new ImplicitContainerExtractorTestModelProvider() );
	}

	private static final class ImplicitContainerExtractorTestModelProvider implements TestModelProvider {

		@Override
		public TestModel<?, String[]> objectArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				private Integer id;
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField
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
				@GenericField(valueBridge = @ValueBridgeRef(type = PrefixedStringBridge.class))
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
				@GenericField
				private List<MyEnum> myProperty;

				private IndexedEntity(int id, List<MyEnum> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}
	}
}
