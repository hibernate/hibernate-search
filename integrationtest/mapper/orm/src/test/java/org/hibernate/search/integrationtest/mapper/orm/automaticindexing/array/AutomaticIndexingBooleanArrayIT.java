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
public class AutomaticIndexingBooleanArrayIT extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingBooleanArrayIT.IndexedEntity, boolean[], Boolean
		> {

	public AutomaticIndexingBooleanArrayIT() {
		super( new BooleanArrayModelPrimitives() );
	}

	private static class BooleanArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, boolean[], Boolean> {

		private static final List<Boolean> VALUES = Arrays.asList( true, false, true, false );

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
		public boolean[] newArray(int size) {
			return new boolean[size];
		}

		@Override
		public void setElement(boolean[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<Boolean> getExpectedIndexFieldType() {
			return Boolean.class;
		}

		@Override
		public Boolean getExpectedIndexFieldValue(boolean[] array, int arrayIndex) {
			return array[arrayIndex];
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, boolean[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public boolean[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, boolean[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public boolean[] getElementCollectionArray(IndexedEntity indexedEntity) {
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
		private boolean[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private boolean[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public boolean[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(boolean[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public boolean[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(boolean[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
