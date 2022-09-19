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
public class AutomaticIndexingByteArrayIT extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingByteArrayIT.IndexedEntity, byte[], Byte
		> {

	public AutomaticIndexingByteArrayIT() {
		super( new ByteArrayModelPrimitives() );
	}

	private static class ByteArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, byte[], Byte> {

		private static final List<Byte> VALUES = Arrays.asList( (byte) 0, (byte) 23, (byte) 121, (byte) -12 );

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
		public byte[] newArray(int size) {
			return new byte[size];
		}

		@Override
		public void setElement(byte[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<Byte> getExpectedIndexFieldType() {
			return Byte.class;
		}

		@Override
		public Byte getExpectedIndexFieldValue(byte[] array, int arrayIndex) {
			return array[arrayIndex];
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, byte[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public byte[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, byte[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public byte[] getElementCollectionArray(IndexedEntity indexedEntity) {
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
		private byte[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private byte[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public byte[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(byte[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public byte[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(byte[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
