/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerReference;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.schema.impl.model.FieldDataType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.schema.impl.model.NormsType;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.PartialDocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * An {@link ElasticsearchSchemaTranslator} implementation for Elasticsearch 5.0/5.1.
 *
 * @author Yoann Rodiere
 */
public class Elasticsearch50SchemaTranslator extends Elasticsearch2SchemaTranslator {

	private static final Log log = LoggerFactory.make( Log.class );

	@Override
	public boolean isTextDataType(PartialDocumentFieldMetadata fieldMetadata) {
		DataType stringDataType = getStringType( fieldMetadata.getPath().getAbsoluteName(),
				fieldMetadata.getIndex(), fieldMetadata.getAnalyzerReference() );
		if ( DataType.TEXT.equals( stringDataType ) ) {
			// Also check that this is actually a string field
			ExtendedFieldType fieldType = FieldHelper.getType( fieldMetadata );
			if ( ExtendedFieldType.STRING.equals( fieldType )
					// We also use strings when the type is unknown
					|| ExtendedFieldType.UNKNOWN.equals( fieldType ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected PropertyMapping generateTenantIdProperty() {
		PropertyMapping tenantId = new PropertyMapping();
		tenantId.setType( DataType.KEYWORD );
		tenantId.setIndex( IndexType.TRUE );
		return tenantId;
	}

	@Override
	protected void addSubfieldIndexOptions(PropertyMapping fieldMapping, FacetMetadata facetMetadata) {
		fieldMapping.setIndex( IndexType.TRUE );
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void addIndexOptions(PropertyMapping propertyMapping, ElasticsearchMappingBuilder mappingBuilder,
			ElasticsearchIndexSettingsBuilder settingsBuilder, PropertyMetadata sourceProperty,
			String propertyPath, Field.Index index, AnalyzerReference analyzerReference) {
		propertyMapping.setIndex( index.isIndexed() ? IndexType.TRUE : IndexType.FALSE );

		DataType type = propertyMapping.getType();

		if ( FieldHelper.isSortableField( mappingBuilder.getMetadata(), sourceProperty, propertyPath ) ) {
			if ( DataType.TEXT.equals( type ) ) {
				// Text fields do not support sort by default, and do not support doc values
				propertyMapping.setFieldData( FieldDataType.TRUE );
			}
			else if ( !index.isIndexed() ) {
				// We must use doc values in order to enable sorting on non-indexed fields
				propertyMapping.setDocValues( true );
			}
		}

		addAnalyzerOptions( propertyMapping, mappingBuilder, settingsBuilder,
				propertyPath, index, analyzerReference );

		// Only text and keyword fields can have norms
		if ( DataType.TEXT.equals( type ) || DataType.KEYWORD.equals( type ) ) {
			propertyMapping.setNorms( index.omitNorms() ? NormsType.FALSE : NormsType.TRUE );
		}
	}

	protected void addAnalyzerOptions(PropertyMapping propertyMapping, ElasticsearchMappingBuilder mappingBuilder,
			ElasticsearchIndexSettingsBuilder settingsBuilder, String propertyPath,
			Field.Index index, AnalyzerReference analyzerReference) {
		DataType type = propertyMapping.getType();

		// Only text fields can be analyzed
		if ( DataType.TEXT.equals( type ) && analyzerReference != null ) {
			if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
				log.analyzerIsNotElasticsearch( mappingBuilder.getTypeIdentifier(), propertyPath, analyzerReference );
			}
			else {
				ElasticsearchAnalyzerReference elasticsearchReference = analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
				String analyzerName = settingsBuilder.register( elasticsearchReference, propertyPath );
				propertyMapping.setAnalyzer( analyzerName );
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected DataType getStringType(String propertyPath, Index index, AnalyzerReference analyzerReference) {
		return index.isAnalyzed() ? DataType.TEXT : DataType.KEYWORD;
	}

}
