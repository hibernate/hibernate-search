/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerReference;
import org.hibernate.search.elasticsearch.bridge.builtin.impl.ElasticsearchBridgeDefinedField;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.impl.ToElasticsearch;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchAsNullNullMarkerCodec;
import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.schema.impl.model.DynamicType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.DocumentFieldPath;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.PartialDocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonPrimitive;

/**
 * An {@link ElasticsearchSchemaTranslator} implementation for Elasticsearch 2.
 *
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
@SuppressWarnings( "deprecation" )
public class Elasticsearch2SchemaTranslator implements ElasticsearchSchemaTranslator {

	private static final Log LOG = LoggerFactory.make( Log.class );

	@Override
	public IndexMetadata translate(URLEncodedString indexName, Collection<EntityIndexBinding> descriptors, ExecutionOptions executionOptions) {
		IndexMetadata indexMetadata = new IndexMetadata();

		indexMetadata.setName( indexName );

		ElasticsearchIndexSettingsBuilder settingsBuilder = new ElasticsearchIndexSettingsBuilder();
		for ( EntityIndexBinding descriptor : descriptors ) {
			String typeName = descriptor.getDocumentBuilder().getTypeIdentifier().getName();

			TypeMapping mapping = translate( descriptor, settingsBuilder, executionOptions );

			indexMetadata.putMapping( typeName, mapping );
		}

		indexMetadata.setSettings( settingsBuilder.build() );

		return indexMetadata;
	}

	@Override
	public boolean isTextDataType(PartialDocumentFieldMetadata fieldMetadata) {
		// This datatype does not exist on Elasticsearch 2.x
		return false;
	}

	private TypeMapping translate(EntityIndexBinding descriptor, ElasticsearchIndexSettingsBuilder settingsBuilder, ExecutionOptions executionOptions) {
		TypeMapping root = new TypeMapping();

		root.setDynamic( executionOptions.getDynamicMapping() );

		ElasticsearchMappingBuilder mappingBuilder = new ElasticsearchMappingBuilder( descriptor, root );

		if ( executionOptions.isMultitenancyEnabled() ) {
			PropertyMapping tenantId = generateTenantIdProperty();
			mappingBuilder.setPropertyAbsolute( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId );
		}

		addMappings( mappingBuilder, settingsBuilder, executionOptions );

		return root;
	}

	protected PropertyMapping generateTenantIdProperty() {
		PropertyMapping tenantId = new PropertyMapping();
		tenantId.setType( DataType.STRING );
		tenantId.setIndex( IndexType.NOT_ANALYZED );
		return tenantId;
	}

	private void addMappings(ElasticsearchMappingBuilder mappingBuilder, ElasticsearchIndexSettingsBuilder settingsBuilder,
			ExecutionOptions executionOptions) {
		TypeMetadata typeMetadata = mappingBuilder.getMetadata();

		// normal document fields
		for ( DocumentFieldMetadata fieldMetadata : typeMetadata.getNonEmbeddedDocumentFieldMetadata() ) {
			addPropertyMapping( mappingBuilder, settingsBuilder, fieldMetadata, executionOptions );
		}

		// bridge-defined fields
		for ( BridgeDefinedField bridgeDefinedField : getNonEmbeddedBridgeDefinedFields( typeMetadata ) ) {
			addPropertyMapping( mappingBuilder, settingsBuilder, bridgeDefinedField );
		}

		// Recurse into embedded types
		for ( EmbeddedTypeMetadata embeddedTypeMetadata : typeMetadata.getEmbeddedTypeMetadata() ) {
			ElasticsearchMappingBuilder embeddedContext = new ElasticsearchMappingBuilder( mappingBuilder, embeddedTypeMetadata );
			addMappings( embeddedContext, settingsBuilder, executionOptions );
		}
	}

	/**
	 * Adds a property mapping for the given field to the given type mapping.
	 * @param executionOptions
	 */
	private void addPropertyMapping(ElasticsearchMappingBuilder mappingBuilder, ElasticsearchIndexSettingsBuilder settingsBuilder,
			DocumentFieldMetadata fieldMetadata, ExecutionOptions executionOptions) {
		if ( fieldMetadata.getAbsoluteName().isEmpty() || fieldMetadata.getAbsoluteName().endsWith( "." )
				|| fieldMetadata.isSpatial() ) {
			return;
		}

		String propertyPath = fieldMetadata.getAbsoluteName();

		PropertyMapping propertyMapping = new PropertyMapping();

		addTypeOptions( mappingBuilder, propertyMapping, fieldMetadata );

		if ( propertyMapping.getType() != DataType.OBJECT ) {
			propertyMapping.setStore( fieldMetadata.getStore() == Store.NO ? false : true );

			addIndexOptions( propertyMapping, mappingBuilder, settingsBuilder,
					fieldMetadata.getSourceProperty(), fieldMetadata.getAbsoluteName(),
					fieldMetadata.getIndex(), fieldMetadata.getAnalyzerReference() );

			propertyMapping.setBoost( mappingBuilder.getBoost( fieldMetadata.getBoost() ) );
		}

		logDynamicBoostWarning( mappingBuilder, fieldMetadata.getSourceType().getDynamicBoost(), propertyPath );
		PropertyMetadata sourceProperty = fieldMetadata.getSourceProperty();
		if ( sourceProperty != null ) {
			logDynamicBoostWarning( mappingBuilder, sourceProperty.getDynamicBoostStrategy(), propertyPath );
		}

		addNullValue( propertyMapping, mappingBuilder, fieldMetadata );

		for ( FacetMetadata facetMetadata : fieldMetadata.getFacetMetadata() ) {
			addSubfieldMapping( propertyMapping, mappingBuilder, facetMetadata );
		}

		// Do this last, when we're sure no exception will be thrown for this mapping
		mappingBuilder.setPropertyAbsolute( propertyPath, propertyMapping );
	}

	private void logDynamicBoostWarning(ElasticsearchMappingBuilder mappingBuilder, BoostStrategy dynamicBoostStrategy, String fieldPath) {
		if ( dynamicBoostStrategy != null && !DefaultBoostStrategy.INSTANCE.equals( dynamicBoostStrategy ) ) {
			LOG.unsupportedDynamicBoost( dynamicBoostStrategy.getClass(), mappingBuilder.getTypeIdentifier(), fieldPath );
		}
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private void addPropertyMapping(ElasticsearchMappingBuilder mappingBuilder, ElasticsearchIndexSettingsBuilder settingsBuilder,
			BridgeDefinedField bridgeDefinedField) {
		String propertyPath = bridgeDefinedField.getAbsoluteName();

		if ( SpatialHelper.isSpatialFieldLongitude( propertyPath ) ) {
			// we ignore the longitude field, we will create the geo_point mapping only once with the latitude field
			return;
		}
		else if ( SpatialHelper.isSpatialFieldLatitude( propertyPath ) ) {
			// we only add the geo_point for the latitude field
			PropertyMapping propertyMapping = new PropertyMapping();

			propertyMapping.setType( DataType.GEO_POINT );

			// in this case, the spatial field has precedence over an already defined field
			mappingBuilder.setPropertyAbsolute( SpatialHelper.stripSpatialFieldSuffix( propertyPath ), propertyMapping );
		}
		else {
			// This includes the fields potentially created for the spatial hash queries

			// we don't overwrite already defined fields. Typically, in the case of spatial, the geo_point field
			// is defined before the double field and we want to keep the geo_point one
			if ( !mappingBuilder.hasPropertyAbsolute( propertyPath ) ) {
				PropertyMapping propertyMapping = new PropertyMapping();
				addTypeOptions( mappingBuilder, propertyMapping, bridgeDefinedField );

				if ( propertyMapping.getType() != DataType.OBJECT ) {
					addIndexOptions( propertyMapping, mappingBuilder, settingsBuilder,
							bridgeDefinedField.getSourceField().getSourceProperty(),
							propertyPath, bridgeDefinedField.getIndex(), null );
				}

				addDynamicOption( bridgeDefinedField, propertyMapping );
				mappingBuilder.setPropertyAbsolute( propertyPath, propertyMapping );
			}
			else {
				TypeMapping propertyMapping = mappingBuilder.getPropertyAbsolute( propertyPath );
				addDynamicOption( bridgeDefinedField, propertyMapping );
			}
		}
	}

	private void addDynamicOption(BridgeDefinedField bridgeDefinedField, TypeMapping propertyMapping) {
		ElasticsearchBridgeDefinedField mappedOn = bridgeDefinedField.getBridgeDefinedField( ElasticsearchBridgeDefinedField.class );
		if ( mappedOn != null && mappedOn.getDynamic() != null ) {
			propertyMapping.setDynamic( DynamicType.valueOf( mappedOn.getDynamic().name().toUpperCase( Locale.ROOT ) ) );
		}
	}

	/**
	 * Adds an Elasticsearch "field" to an existing property for a facet.
	 * <p>Note that "field" in ES has a very specific meaning, which is not the meaning it has in Lucene or Hibernate Search.
	 */
	private void addSubfieldMapping(PropertyMapping propertyMapping, ElasticsearchMappingBuilder mappingBuilder, FacetMetadata facetMetadata) {
		String facetFieldName = facetMetadata.getPath().getRelativeName() + ToElasticsearch.FACET_FIELD_SUFFIX;

		PropertyMapping fieldMapping = new PropertyMapping();

		addTypeOptions( mappingBuilder, fieldMapping, facetMetadata, Field.Index.NOT_ANALYZED, null );
		fieldMapping.setStore( false );
		addSubfieldIndexOptions( fieldMapping, facetMetadata );

		// Do this last, when we're sure no exception will be thrown for this mapping
		propertyMapping.addField( facetFieldName, fieldMapping );
	}

	/*
	 * Adds the main indexing-related options to the given facet sub-field: "index", "doc_values", "analyzer", ...
	 */
	protected void addSubfieldIndexOptions(PropertyMapping fieldMapping, FacetMetadata facetMetadata) {
		fieldMapping.setIndex( IndexType.NOT_ANALYZED );
	}

	/*
	 * Adds the main indexing-related options to the given property: "index", "doc_values", "analyzer", ...
	 */
	protected void addIndexOptions(PropertyMapping propertyMapping, ElasticsearchMappingBuilder mappingBuilder,
			ElasticsearchIndexSettingsBuilder settingsBuilder, PropertyMetadata sourceProperty,
			String propertyPath, Field.Index index, AnalyzerReference analyzerReference) {
		IndexType elasticsearchIndex = elasticsearchIndexType( propertyMapping, index );
		propertyMapping.setIndex( elasticsearchIndex );

		if ( IndexType.NO.equals( elasticsearchIndex ) && FieldHelper.isSortableField( mappingBuilder.getMetadata(), sourceProperty, propertyPath ) ) {
			// We must use doc values in order to enable sorting on non-indexed fields
			propertyMapping.setDocValues( true );
		}

		if ( IndexType.ANALYZED.equals( elasticsearchIndex ) && analyzerReference != null ) {
			if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
				LOG.analyzerIsNotElasticsearch( mappingBuilder.getTypeIdentifier(), propertyPath, analyzerReference );
			}
			else {
				ElasticsearchAnalyzerReference elasticsearchReference = analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
				String analyzerName = settingsBuilder.register( elasticsearchReference, propertyPath );
				propertyMapping.setAnalyzer( analyzerName );
			}
		}
	}

	private IndexType elasticsearchIndexType(PropertyMapping propertyMapping, Field.Index index) {
		switch ( index ) {
			case ANALYZED:
			case ANALYZED_NO_NORMS:
				return canTypeBeAnalyzed( propertyMapping.getType() )
						? IndexType.ANALYZED
						: IndexType.NOT_ANALYZED;
			case NOT_ANALYZED:
			case NOT_ANALYZED_NO_NORMS:
				return IndexType.NOT_ANALYZED;
			case NO:
				return IndexType.NO;
			default:
				throw new AssertionFailure( "Unexpected index type: " + index );
		}
	}

	private boolean canTypeBeAnalyzed(DataType fieldType) {
		return DataType.STRING.equals( fieldType );
	}

	private void addTypeOptions(ElasticsearchMappingBuilder mappingBuilder,
			PropertyMapping propertyMapping, DocumentFieldMetadata fieldMetadata) {
		addTypeOptions( mappingBuilder, fieldMetadata.getAbsoluteName(), propertyMapping, FieldHelper.getType( fieldMetadata ),
				fieldMetadata.getIndex(), fieldMetadata.getAnalyzerReference() );
	}

	private void addTypeOptions(ElasticsearchMappingBuilder mappingBuilder,
			PropertyMapping propertyMapping, BridgeDefinedField bridgeDefinedField) {
		ExtendedFieldType type = FieldHelper.getType( bridgeDefinedField );

		if ( ExtendedFieldType.UNKNOWN.equals( type ) ) {
			throw LOG.unexpectedFieldType( bridgeDefinedField.getType().name(), bridgeDefinedField.getAbsoluteName() );
		}

		addTypeOptions( mappingBuilder, bridgeDefinedField.getAbsoluteName(), propertyMapping, type,
				bridgeDefinedField.getIndex(), bridgeDefinedField.getSourceField().getAnalyzerReference() );
	}

	private void addTypeOptions(ElasticsearchMappingBuilder mappingBuilder,
			PropertyMapping fieldMapping, FacetMetadata facetMetadata,
			Field.Index index, AnalyzerReference analyzerReference) {
		ExtendedFieldType type;

		if ( facetMetadata.isEncodingAuto() ) {
			/*
			 * If the user didn't ask for a specific encoding, just use the same datatype
			 * as the source field.
			 */
			addTypeOptions( mappingBuilder, fieldMapping, facetMetadata.getSourceField() );
			return;
		}

		switch ( facetMetadata.getEncoding() ) {
			case DOUBLE:
				type = ExtendedFieldType.DOUBLE;
				break;
			case LONG:
				type = ExtendedFieldType.LONG;
				break;
			case STRING:
				type = ExtendedFieldType.STRING;
				break;
			case AUTO:
				throw new AssertionFailure( "The facet type should have been resolved during bootstrapping" );
			default: {
				throw new AssertionFailure(
						"Unexpected facet encoding type '"
								+ facetMetadata.getEncoding()
								+ "' Has the enum been modified?"
				);
			}
		}

		addTypeOptions( mappingBuilder, facetMetadata.getAbsoluteName(), fieldMapping, type,
				index, analyzerReference );
	}

	private DataType addTypeOptions(ElasticsearchMappingBuilder mappingBuilder,
			String fieldName, PropertyMapping propertyMapping, ExtendedFieldType extendedType,
			Field.Index index, AnalyzerReference analyzerReference) {
		DataType elasticsearchType;
		List<String> formats = new ArrayList<>();

		/* Note: for date formats, we use a 4-digit year format as the first format
		 * (which is the output format), so that Elasticsearch outputs are more
		 * human-readable.
		 */
		switch ( extendedType ) {
			case BOOLEAN:
				elasticsearchType = DataType.BOOLEAN;
				break;
			case CALENDAR:
			case DATE:
			case INSTANT:
				elasticsearchType = DataType.DATE;
				// Use default formats ("strict_date_optional_time||epoch_millis")
				break;
			case LOCAL_DATE:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_date" );
				formats.add( "yyyyyyyyy-MM-dd" );
				break;
			case LOCAL_DATE_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_date_hour_minute_second_fraction" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS" );
				break;
			case LOCAL_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_hour_minute_second_fraction" );
				break;
			case OFFSET_DATE_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_date_time" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ" );
				break;
			case OFFSET_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_time" );
				break;
			case ZONED_DATE_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "yyyy-MM-dd'T'HH:mm:ss.SSSZZ'['ZZZ']'" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ'['ZZZ']'" );
				break;
			case YEAR:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_year" );
				formats.add( "yyyyyyyyy" );
				break;
			case YEAR_MONTH:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_year_month" );
				formats.add( "yyyyyyyyy-MM" );
				break;
			case MONTH_DAY:
				elasticsearchType = DataType.DATE;
				/*
				 * This seems to be the ISO-8601 format for dates without year.
				 * It's also the default format for Java's MonthDay, see MonthDay.PARSER.
				 */
				formats.add( "--MM-dd" );
				break;
			case INTEGER:
				elasticsearchType = DataType.INTEGER;
				break;
			case LONG:
				elasticsearchType = DataType.LONG;
				break;
			case FLOAT:
				elasticsearchType = DataType.FLOAT;
				break;
			case DOUBLE:
				elasticsearchType = DataType.DOUBLE;
				break;
			case OBJECT:
				elasticsearchType = DataType.OBJECT;
				break;
			case UNKNOWN_NUMERIC:
				throw LOG.unexpectedNumericEncodingType( mappingBuilder.getTypeIdentifier(), fieldName );
			case STRING:
			case UNKNOWN:
			default:
				elasticsearchType = getStringType( fieldName, index, analyzerReference );
				break;
		}

		propertyMapping.setType( elasticsearchType );

		if ( !formats.isEmpty() ) {
			propertyMapping.setFormat( formats );
		}

		return elasticsearchType;
	}

	protected DataType getStringType(String propertyPath, Index index, AnalyzerReference analyzerReference) {
		return DataType.STRING;
	}

	private void addNullValue(PropertyMapping propertyMapping, ElasticsearchMappingBuilder mappingBuilder, DocumentFieldMetadata fieldMetadata) {
		NullMarkerCodec nullMarkerCodec = fieldMetadata.getNullMarkerCodec();
		/*
		 * We may have a codec that doesn't index null values as null,
		 * because 'null_value' is not supported for the 'text' datatype.
		 * See ElasticsearchMissingValueStrategy.
		 */
		if ( nullMarkerCodec instanceof ElasticsearchAsNullNullMarkerCodec ) {
			NullMarker nullMarker = nullMarkerCodec.getNullMarker();
			JsonPrimitive nullTokenJson = convertIndexedNullTokenToJson( mappingBuilder,
					fieldMetadata.getPath(), nullMarker.nullEncoded() );
			propertyMapping.setNullValue( nullTokenJson );
		}
	}

	private JsonPrimitive convertIndexedNullTokenToJson(ElasticsearchMappingBuilder mappingBuilder,
			DocumentFieldPath fieldPath, Object indexedNullToken) {
		if ( indexedNullToken == null ) {
			return null;
		}

		if ( indexedNullToken instanceof String ) {
			return new JsonPrimitive( (String) indexedNullToken );
		}
		else if ( indexedNullToken instanceof Number ) {
			return new JsonPrimitive( (Number) indexedNullToken );
		}
		else if ( indexedNullToken instanceof Boolean ) {
			return new JsonPrimitive( (Boolean) indexedNullToken );
		}
		else {
			throw LOG.unsupportedNullTokenType( mappingBuilder.getTypeIdentifier(), fieldPath.getAbsoluteName(),
					indexedNullToken.getClass() );
		}
	}

	/**
	 * Collects all the bridge-defined fields for the given type, excluding its embedded types.
	 */
	private Set<BridgeDefinedField> getNonEmbeddedBridgeDefinedFields(TypeMetadata type) {
		Set<BridgeDefinedField> bridgeDefinedFields = new HashSet<>();
		for ( DocumentFieldMetadata documentFieldMetadata : type.getNonEmbeddedDocumentFieldMetadata() ) {
			bridgeDefinedFields.addAll( documentFieldMetadata.getBridgeDefinedFields().values() );
		}
		return bridgeDefinedFields;
	}

}
