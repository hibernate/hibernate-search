/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.array;

import java.util.Arrays;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class AutomaticIndexingStringArrayIT extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingStringArrayIT.IndexedEntity, String[], String
		> {

	public AutomaticIndexingStringArrayIT() {
		super( new StringArrayModelPrimitives() );
	}

	private static class StringArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, String[], String> {

		private static final List<String> VALUES = Arrays.asList( "first value", "second value", "third value",
				"fourth value" );

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
		public String[] newArray(int size) {
			return new String[size];
		}

		@Override
		public void setElement(String[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<String> getExpectedIndexFieldType() {
			return String.class;
		}

		@Override
		public String getExpectedIndexFieldValue(String[] array, int arrayIndex) {
			return array[arrayIndex];
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, String[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public String[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, String[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public String[] getElementCollectionArray(IndexedEntity indexedEntity) {
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
		private String[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private String[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public String[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(String[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public String[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(String[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
