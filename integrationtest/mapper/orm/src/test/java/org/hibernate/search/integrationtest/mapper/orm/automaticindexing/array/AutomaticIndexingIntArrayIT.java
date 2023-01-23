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
public class AutomaticIndexingIntArrayIT extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingIntArrayIT.IndexedEntity, int[], Integer
		> {

	public AutomaticIndexingIntArrayIT() {
		super( new IntArrayModelPrimitives() );
	}

	private static class IntArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, int[], Integer> {

		private static final List<Integer> VALUES = Arrays.asList( 123, 245, 233, 566, 5423 );

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
		public int[] newArray(int size) {
			return new int[size];
		}

		@Override
		public void setElement(int[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<Integer> getExpectedIndexFieldType() {
			return Integer.class;
		}

		@Override
		public Integer getExpectedIndexFieldValue(int[] array, int arrayIndex) {
			return array[arrayIndex];
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, int[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public int[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, int[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public int[] getElementCollectionArray(IndexedEntity indexedEntity) {
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
		private int[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private int[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public int[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(int[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public int[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(int[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
