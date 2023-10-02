/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
class AutomaticIndexingCharArrayIT
		extends AbstractAutomaticIndexingArrayIT<
				AutomaticIndexingCharArrayIT.IndexedEntity,
				char[],
				String> {

	public AutomaticIndexingCharArrayIT() {
		super( new CharArrayModelPrimitives() );
	}

	private static class CharArrayModelPrimitives
			implements ArrayModelPrimitives<IndexedEntity, char[], String> {

		private static final List<Character> VALUES = Arrays.asList( 'a', 'b', 'c', 'd' );

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
		public char[] newArray(int size) {
			return new char[size];
		}

		@Override
		public void setElement(char[] array, int arrayIndex, int valueOrdinal) {
			array[arrayIndex] = VALUES.get( valueOrdinal );
		}

		@Override
		public Class<String> getExpectedIndexFieldType() {
			return String.class;
		}

		@Override
		public String getExpectedIndexFieldValue(char[] array, int arrayIndex) {
			return Character.toString( array[arrayIndex] );
		}

		@Override
		public void setSerializedArray(IndexedEntity indexed, char[] array) {
			indexed.setSerializedArray( array );
		}

		@Override
		public char[] getSerializedArray(IndexedEntity indexed) {
			return indexed.getSerializedArray();
		}

		@Override
		public void setElementCollectionArray(IndexedEntity indexedEntity, char[] array) {
			indexedEntity.setElementCollectionArray( array );
		}

		@Override
		public char[] getElementCollectionArray(IndexedEntity indexedEntity) {
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
		private char[] serializedArray;

		@GenericField
		@ElementCollection
		@OrderColumn
		private char[] elementCollectionArray;

		protected IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public char[] getSerializedArray() {
			return serializedArray;
		}

		public void setSerializedArray(char[] serializedArray) {
			this.serializedArray = serializedArray;
		}

		public char[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(char[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}
	}

}
