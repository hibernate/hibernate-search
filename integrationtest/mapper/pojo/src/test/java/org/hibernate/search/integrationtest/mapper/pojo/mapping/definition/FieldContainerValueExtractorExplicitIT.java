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
import java.util.OptionalInt;
import java.util.Set;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.extractor.builtin.ArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.IterableElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.MapKeyExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.OptionalIntValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.OptionalValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerValueExtractorBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeBeanReference;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Test explicit container value extractors for the {@code @Field} annotation.
 * <p>
 * Error cases are not tested here but in {@link FieldContainerValueExtractorBaseIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-2554")
public class FieldContainerValueExtractorExplicitIT extends AbstractFieldContainerValueExtractorIT {

	public FieldContainerValueExtractorExplicitIT() {
		super( new ExplicitContainerValueExtractorTestModelProvider() );
	}

	@Test
	public void mapKeys() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			private Integer id;
			private Map<String, String> myProperty;

			IndexedEntity(int id, Map<String, String> myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}

			@DocumentId
			public Integer getId() {
				return id;
			}

			@Field(extractors = @ContainerValueExtractorBeanReference(type = MapKeyExtractor.class))
			public Map<String, String> getMyProperty() {
				return myProperty;
			}
		}
		Map<String, String> map = new LinkedHashMap<>();
		map.put( STRING_VALUE_1, STRING_VALUE_4 );
		map.put( STRING_VALUE_2, STRING_VALUE_5 );
		map.put( STRING_VALUE_3, STRING_VALUE_6 );
		doTest(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				String.class,
				map,
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3
		);
	}

	@Test
	public void chain_mapListKeys() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			private Integer id;
			private Map<List<String>, String> myProperty;

			private IndexedEntity(int id, Map<List<String>, String> myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}

			@DocumentId
			public Integer getId() {
				return id;
			}

			@Field(extractors = {
					@ContainerValueExtractorBeanReference(type = MapKeyExtractor.class),
					@ContainerValueExtractorBeanReference(type = IterableElementExtractor.class)
			})
			public Map<List<String>, String> getMyProperty() {
				return myProperty;
			}
		}
		Map<List<String>, String> map = new LinkedHashMap<>();
		map.put( CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2 ), STRING_VALUE_3 );
		map.put( CollectionHelper.asList( STRING_VALUE_4, STRING_VALUE_5 ), STRING_VALUE_6 );
		doTest(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				String.class,
				map,
				STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_4, STRING_VALUE_5
		);
	}

	@Test
	public void containerBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			private Integer id;
			private List<String> myProperty;

			private IndexedEntity(int id, List<String> myProperty) {
				this.id = id;
				this.myProperty = myProperty;
			}

			@DocumentId
			public Integer getId() {
				return id;
			}

			@Field(extractors = {}, valueBridge = @ValueBridgeBeanReference(type = FirstCollectionElementBridge.class))
			public List<String> getMyProperty() {
				return myProperty;
			}
		}
		doTest(
				IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ),
				String.class,
				CollectionHelper.asList( STRING_VALUE_1, STRING_VALUE_2, STRING_VALUE_3 ),
				STRING_VALUE_1
		);
	}

	private static final class ExplicitContainerValueExtractorTestModelProvider implements TestModelProvider {

		@Override
		public TestModel<?, String[]> objectArray() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private String[] myProperty;

				private IndexedEntity(int id, String[] myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = ArrayElementExtractor.class))
				public String[] getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Iterable<String>> iterable() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private Iterable<String> myProperty;

				private IndexedEntity(int id, Iterable<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = IterableElementExtractor.class))
				public Iterable<String> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Collection<String>> collection() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private Collection<String> myProperty;

				private IndexedEntity(int id, Collection<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = IterableElementExtractor.class))
				public Collection<String> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, List<String>> list() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private List<String> myProperty;

				private IndexedEntity(int id, List<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = IterableElementExtractor.class))
				public List<String> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Set<String>> set() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private Set<String> myProperty;

				private IndexedEntity(int id, Set<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = IterableElementExtractor.class))
				public Set<String> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Map<String, String>> mapValues() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private Map<String, String> myProperty;

				private IndexedEntity(int id, Map<String, String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = MapValueExtractor.class))
				public Map<String, String> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Map<String, List<String>>> mapListValues() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private Map<String, List<String>> myProperty;

				private IndexedEntity(int id, Map<String, List<String>> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = {
						@ContainerValueExtractorBeanReference(type = MapValueExtractor.class),
						@ContainerValueExtractorBeanReference(type = IterableElementExtractor.class)
				})
				public Map<String, List<String>> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, Optional<String>> optional() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private Optional<String> myProperty;

				private IndexedEntity(int id, Optional<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = OptionalValueExtractor.class))
				public Optional<String> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, OptionalInt> optionalInt() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private OptionalInt myProperty;

				private IndexedEntity(int id, OptionalInt myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = OptionalIntValueExtractor.class))
				public OptionalInt getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, List<String>> list_explicitPrefixedStringBridge() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private List<String> myProperty;

				private IndexedEntity(int id, List<String> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(
						valueBridge = @ValueBridgeBeanReference(type = PrefixedStringBridge.class),
						extractors = @ContainerValueExtractorBeanReference(type = IterableElementExtractor.class)
				)
				public List<String> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}

		@Override
		public TestModel<?, List<MyEnum>> list_implicitEnumBridge() {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				private Integer id;
				private List<MyEnum> myProperty;

				private IndexedEntity(int id, List<MyEnum> myProperty) {
					this.id = id;
					this.myProperty = myProperty;
				}

				@DocumentId
				public Integer getId() {
					return id;
				}

				@Field(extractors = @ContainerValueExtractorBeanReference(type = IterableElementExtractor.class))
				public List<MyEnum> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}
	}

	public static class FirstCollectionElementBridge implements ValueBridge<Collection<String>, String> {
		@Override
		public String toIndexedValue(Collection<String> value) {
			return value == null || value.isEmpty() ? null : value.iterator().next();
		}
		@Override
		public Collection<String> cast(Object value) {
			return (Collection<String>) value;
		}
	}

}
