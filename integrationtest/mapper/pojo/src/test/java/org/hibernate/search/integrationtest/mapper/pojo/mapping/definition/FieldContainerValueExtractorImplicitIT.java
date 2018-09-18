/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeBeanReference;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

/**
 * Test implicit container value extractors for the {@code @Field} annotation.
 * <p>
 * Error cases are not tested here but in {@link FieldContainerValueExtractorBaseIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-2554")
public class FieldContainerValueExtractorImplicitIT extends AbstractFieldContainerValueExtractorIT {

	public FieldContainerValueExtractorImplicitIT() {
		super( new ImplicitContainerValueExtractorTestModelProvider() );
	}

	private static final class ImplicitContainerValueExtractorTestModelProvider implements TestModelProvider {

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

				@Field
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

				@Field
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

				@Field
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

				@Field
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

				@Field
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

				@Field
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

				@Field
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

				@Field
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

				@Field
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

				@Field(valueBridge = @ValueBridgeBeanReference(type = PrefixedStringBridge.class))
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

				@Field
				public List<MyEnum> getMyProperty() {
					return myProperty;
				}
			}
			return new TestModel<>( IndexedEntity.class, (id, p) -> new IndexedEntity( id, p ) );
		}
	}
}
