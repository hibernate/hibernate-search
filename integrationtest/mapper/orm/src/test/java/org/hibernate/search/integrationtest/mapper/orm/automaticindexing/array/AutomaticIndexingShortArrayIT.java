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
public class AutomaticIndexingShortArrayIT extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingShortArrayIT.IndexedEntity, short[], Short
		> {

	public AutomaticIndexingShortArrayIT() {
		super( new ShortArrayModelPrimitives() );
	}

	private static class ShortArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, short[], Short> {

		private static final List<Short> VALUES = Arrays.asList( (short) 0, (short) 12, (short) 14536, (short) -14 );

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
		public short[] newArray(int size) {
			return new short[size];
		}

		@Override
		public void setElement(short[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<Short> getExpectedIndexFieldType() {
			return Short.class;
		}

		@Override
		public Short getExpectedIndexFieldValue(short[] array, int arrayIndex) {
			return array[arrayIndex];
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, short[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public short[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, short[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public short[] getElementCollectionArray(IndexedEntity indexedEntity) {
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
		private short[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private short[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public short[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(short[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public short[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(short[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
