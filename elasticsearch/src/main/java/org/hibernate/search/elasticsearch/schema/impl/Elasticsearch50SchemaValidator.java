/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.schema.impl.model.FieldDataType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.schema.impl.model.NormsType;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;

import com.google.gson.JsonPrimitive;

/**
 * An {@link ElasticsearchSchemaValidator} implementation for Elasticsearch 5.0/5.1.
 *
 * @author Yoann Rodiere
 */
public class Elasticsearch50SchemaValidator extends Elasticsearch2SchemaValidator {

	public Elasticsearch50SchemaValidator(ElasticsearchSchemaAccessor schemaAccessor) {
		super( schemaAccessor );
	}

	@Override
	protected void doValidateJsonPrimitive(ValidationErrorCollector errorCollector,
			DataType type, String attributeName, JsonPrimitive expectedValue, JsonPrimitive actualValue) {
		switch ( type ) {
			case TEXT:
			case KEYWORD:
				validateEqualWithDefault( errorCollector, attributeName, expectedValue, actualValue, null );
				break;
			default:
				super.doValidateJsonPrimitive( errorCollector, type, attributeName, expectedValue, actualValue );
				break;
		}
	}

	@Override
	protected void validateIndexOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping, PropertyMapping actualMapping) {
		IndexType expectedIndex = expectedMapping.getIndex();
		if ( IndexType.TRUE.equals( expectedIndex ) ) { // If we don't need an index, we don't care
			// From ES 5.0 on, all indexable fields are indexed by default
			IndexType indexDefault = IndexType.TRUE;
			validateEqualWithDefault( errorCollector, "index", expectedIndex, actualMapping.getIndex(), indexDefault );
		}

		NormsType expectedNorms = expectedMapping.getNorms();
		if ( NormsType.TRUE.equals( expectedNorms ) ) { // If we don't need norms, we don't care
			// From ES 5.0 on, norms are enabled by default on text fields only
			NormsType normsDefault = DataType.TEXT.equals( expectedMapping.getType() ) ? NormsType.TRUE : NormsType.FALSE;
			validateEqualWithDefault( errorCollector, "norms", expectedNorms, actualMapping.getNorms(), normsDefault );
		}

		FieldDataType expectedFieldData = expectedMapping.getFieldData();
		if ( FieldDataType.TRUE.equals( expectedFieldData ) ) { // If we don't need an index, we don't care
			validateEqualWithDefault( errorCollector, "fielddata", expectedFieldData, actualMapping.getFieldData(), FieldDataType.FALSE );
		}

		Boolean expectedDocValues = expectedMapping.getDocValues();
		if ( Boolean.TRUE.equals( expectedDocValues ) ) { // If we don't need doc_values, we don't care
			/*
			 * Elasticsearch documentation (2.3) says doc_values is true by default on fields
			 * supporting it, but tests show it's wrong.
			 */
			validateEqualWithDefault( errorCollector, "doc_values", expectedDocValues, actualMapping.getDocValues(), false );
		}
	}
}
