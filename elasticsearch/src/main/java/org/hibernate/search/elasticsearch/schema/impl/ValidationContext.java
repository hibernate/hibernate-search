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
	private final String propertyPath;
	private final String fieldName;

	private final String analyzerName;
	private final String charFilterName;
	private final String tokenizerName;
	private final String tokenFilterName;

	public ValidationContext(String indexName, String mappingName, String propertyPath, String fieldName,
			String analyzerName, String charFilterName, String tokenizerName, String tokenFilterName) {
		super();
		this.indexName = indexName;
		this.mappingName = mappingName;
		this.propertyPath = propertyPath;
		this.fieldName = fieldName;
		this.analyzerName = analyzerName;
		this.charFilterName = charFilterName;
		this.tokenizerName = tokenizerName;
		this.tokenFilterName = tokenFilterName;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getMappingName() {
		return mappingName;
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public String getFieldName() {
		return fieldName;
	}


	public String getAnalyzerName() {
		return analyzerName;
	}


	public String getCharFilterName() {
		return charFilterName;
	}


	public String getTokenizerName() {
		return tokenizerName;
	}


	public String getTokenFilterName() {
		return tokenFilterName;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj != null && getClass().equals( obj.getClass() ) ) {
			ValidationContext other = (ValidationContext) obj;
			return Objects.equals( indexName, other.indexName )
					&& Objects.equals( mappingName, other.mappingName )
					&& Objects.equals( propertyPath, other.propertyPath )
					&& Objects.equals( fieldName, other.fieldName )
					&& Objects.equals( analyzerName, other.analyzerName )
					&& Objects.equals( charFilterName, other.charFilterName )
					&& Objects.equals( tokenizerName, other.tokenizerName )
					&& Objects.equals( tokenFilterName, other.tokenFilterName );
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode( indexName );
		result = prime * result + Objects.hashCode( mappingName );
		result = prime * result + Objects.hashCode( propertyPath );
		result = prime * result + Objects.hashCode( fieldName );
		result = prime * result + Objects.hashCode( analyzerName );
		result = prime * result + Objects.hashCode( charFilterName );
		result = prime * result + Objects.hashCode( tokenizerName );
		result = prime * result + Objects.hashCode( tokenFilterName );
		return result;
	}
}