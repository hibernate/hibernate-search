/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Objects;

final class ValidationContext {
	private final String indexName;
	private final String mappingName;
	private final String path;
	private final String fieldName;

	public ValidationContext(String indexName, String mappingName, String path, String fieldName) {
		super();
		this.indexName = indexName;
		this.mappingName = mappingName;
		this.path = path;
		this.fieldName = fieldName;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getMappingName() {
		return mappingName;
	}

	public String getPath() {
		return path;
	}

	public String getFieldName() {
		return fieldName;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj != null && getClass().equals( obj.getClass() ) ) {
			ValidationContext other = (ValidationContext) obj;
			return Objects.equals( indexName, other.indexName )
					&& Objects.equals( mappingName, other.mappingName )
					&& Objects.equals( path, other.path )
					&& Objects.equals( fieldName, other.fieldName );
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode( indexName );
		result = prime * result + Objects.hashCode( mappingName );
		result = prime * result + Objects.hashCode( path );
		result = prime * result + Objects.hashCode( fieldName );
		return result;
	}
}