/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

/**
 * Meta-data about a sort field of a mapped entity. Sort fields are mapped to doc value fields in the index.
 *
 * @author Gunnar Morling
 */
public class SortableFieldMetadata {

	private final String fieldName;

	private SortableFieldMetadata(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return the name of the field whose field bridge will be used to determine the value for the doc value field.
	 */
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String toString() {
		return "SortableFieldMetadata [fieldName=" + fieldName + "]";
	}

	public static class Builder {

		private String fieldName;

		public Builder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		public SortableFieldMetadata build() {
			return new SortableFieldMetadata( fieldName );
		}
	}
}
