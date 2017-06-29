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
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * An {@link ElasticsearchSchemaTranslator} implementation for Elasticsearch 5.2.
 *
 * @author Yoann Rodiere
 */
public class Elasticsearch52SchemaTranslator extends Elasticsearch50SchemaTranslator {

	private static final Log log = LoggerFactory.make( Log.class );

	@Override
	protected void addAnalyzerOptions(PropertyMapping propertyMapping, ElasticsearchMappingBuilder mappingBuilder,
			ElasticsearchIndexSettingsBuilder settingsBuilder, String propertyPath,
			Field.Index index, AnalyzerReference analyzerReference) {
		super.addAnalyzerOptions( propertyMapping, mappingBuilder, settingsBuilder,
				propertyPath, index, analyzerReference );

		DataType type = propertyMapping.getType();

		// Keyword fields cannot be analyzed, but they can still be normalized
		if ( DataType.KEYWORD.equals( type ) && analyzerReference != null ) {
			if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
				log.analyzerIsNotElasticsearch( mappingBuilder.getTypeIdentifier(), propertyPath, analyzerReference );
			}
			else {
				ElasticsearchAnalyzerReference elasticsearchReference = analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
				if ( elasticsearchReference.isNormalizer( propertyPath ) ) {
					String normalizerName = settingsBuilder.register( elasticsearchReference, propertyPath );
					propertyMapping.setNormalizer( normalizerName );
				}
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected DataType getStringType(String propertyPath, Index index, AnalyzerReference analyzerReference) {
		if ( ! index.isAnalyzed() ) {
			return DataType.KEYWORD;
		}
		if ( analyzerReference != null && analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
			ElasticsearchAnalyzerReference elasticsearchReference = analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
			if ( elasticsearchReference.isNormalizer( propertyPath ) ) {
				return DataType.KEYWORD;
			}
		}
		return DataType.TEXT;
	}

}
