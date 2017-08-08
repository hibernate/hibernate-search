/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class PrimitiveProjection extends FieldProjection {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final TypeMetadata rootTypeMetadata;
	private final String absoluteName;
	private final ExtendedFieldType fieldType;

	public PrimitiveProjection(TypeMetadata rootTypeMetadata, String absoluteName, ExtendedFieldType fieldType) {
		super();
		this.rootTypeMetadata = rootTypeMetadata;
		this.absoluteName = absoluteName;
		this.fieldType = fieldType;
	}

	public void addDocumentField(Document tmp, JsonObject hit, ConversionContext conversionContext) {
		JsonElement jsonValue = extractFieldValue( hit.get( "_source" ).getAsJsonObject(), absoluteName );
		addDocumentField( tmp, jsonValue );
	}

	public void addDocumentField(Document tmp, JsonElement jsonValue) {
		if ( jsonValue == null || jsonValue.isJsonNull() ) {
			return;
		}
		switch ( fieldType ) {
			case INTEGER:
				tmp.add( new IntField( absoluteName, jsonValue.getAsInt(), Store.NO ) );
				break;
			case LONG:
				tmp.add( new LongField( absoluteName, jsonValue.getAsLong(), Store.NO ) );
				break;
			case FLOAT:
				tmp.add( new FloatField( absoluteName, jsonValue.getAsFloat(), Store.NO ) );
				break;
			case DOUBLE:
				tmp.add( new DoubleField( absoluteName, jsonValue.getAsDouble(), Store.NO ) );
				break;
			case UNKNOWN_NUMERIC:
				throw LOG.unexpectedNumericEncodingType( rootTypeMetadata.getType(), absoluteName );
			case BOOLEAN:
				tmp.add( new StringField( absoluteName, String.valueOf( jsonValue.getAsBoolean() ), Store.NO ) );
				break;
			default:
				tmp.add( new StringField( absoluteName, jsonValue.getAsString(), Store.NO ) );
				break;
		}
	}

	@Override
	public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
		JsonElement jsonValue = extractFieldValue( hit.get( "_source" ).getAsJsonObject(), absoluteName );
		if ( jsonValue == null || jsonValue.isJsonNull() ) {
			return null;
		}
		switch ( fieldType ) {
			case INTEGER:
				return jsonValue.getAsInt();
			case LONG:
				return jsonValue.getAsLong();
			case FLOAT:
				return jsonValue.getAsFloat();
			case DOUBLE:
				return jsonValue.getAsDouble();
			case UNKNOWN_NUMERIC:
				throw LOG.unexpectedNumericEncodingType( rootTypeMetadata.getType(), absoluteName );
			case BOOLEAN:
				return jsonValue.getAsBoolean();
			default:
				return jsonValue.getAsString();
		}
	}
}