/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.array;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-3997")
public class AutomaticIndexingFloatArrayIT extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingFloatArrayIT.IndexedEntity, float[], Float
		> {

	public AutomaticIndexingFloatArrayIT() {
		super( new FloatArrayModelPrimitives() );
	}

	private static class FloatArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, float[], Float> {

		private static final List<Float> VALUES = Arrays.asList( 0.0f, 0.25f, 8456.5f, 84.69f );

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
		public float[] newArray(int size) {
			return new float[size];
		}

		@Override
		public void setElement(float[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<Float> getExpectedIndexFieldType() {
			return Float.class;
		}

		@Override
		public Float getExpectedIndexFieldValue(float[] array, int arrayIndex) {
			return array[arrayIndex];
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, float[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public float[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, float[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public float[] getElementCollectionArray(IndexedEntity indexedEntity) {
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
		private float[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private float[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public float[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(float[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public float[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(float[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
