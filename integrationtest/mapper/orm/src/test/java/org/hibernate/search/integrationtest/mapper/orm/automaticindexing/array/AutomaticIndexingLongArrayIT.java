/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.array;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-3997")
public class AutomaticIndexingLongArrayIT extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingLongArrayIT.IndexedEntity, long[], Long
		> {

	public AutomaticIndexingLongArrayIT() {
		super( new LongArrayModelPrimitives() );
	}

	private static class LongArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, long[], Long> {

		private static final List<Long> VALUES = Arrays.asList( 0L, 1L, 1345566L, -4356646443L );

		@Override
		public String getIndexName() {
			return IndexedEntity.NAME;
		}

		@Override
		public Class<IndexedEntity> getIndexedClass() {
			return IndexedEntity.class;
		}

		@Override
		public IndexedEntity newIndexed(int id) {
			return new IndexedEntity( id );
		}

		@Override
		public long[] newArray(int size) {
			return new long[size];
		}

		@Override
		public void setElement(long[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<Long> getExpectedIndexFieldType() {
			return Long.class;
		}

		@Override
		public Long getExpectedIndexFieldValue(long[] array, int arrayIndex) {
			return array[arrayIndex];
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, long[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public long[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, long[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public long[] getElementCollectionArray(IndexedEntity indexedEntity) {
			return indexedEntity.getElementCollectionArray();
		}
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {
		static final String NAME = "Indexed";

		@Id
		private Integer id;

		@GenericField
		@JdbcTypeCode(Types.VARBINARY)
		private long[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private long[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public long[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(long[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public long[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(long[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
