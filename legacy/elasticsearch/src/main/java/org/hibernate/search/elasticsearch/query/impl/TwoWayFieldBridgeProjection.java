/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class TwoWayFieldBridgeProjection extends FieldProjection {

	private final String absoluteName;
	private final TwoWayFieldBridge bridge;
	private final Set<String> objectFieldNames;
	private final Map<String, PrimitiveProjection> primitiveProjections;

	public TwoWayFieldBridgeProjection(String absoluteName,
			TwoWayFieldBridge bridge,
			Set<String> objectFieldNames,
			Map<String, PrimitiveProjection> primitiveProjections) {
		this.absoluteName = absoluteName;
		this.bridge = bridge;
		this.objectFieldNames = objectFieldNames;
		this.primitiveProjections = primitiveProjections;
	}

	@Override
	public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
		return convertFieldValue( hit, conversionContext );
	}

	private Object convertFieldValue(JsonObject hit, ConversionContext conversionContext) {
		Document tmp = new Document();

		for ( String objectFieldName : objectFieldNames ) {
			JsonElement jsonValue = extractFieldValue( hit.get( "_source" ).getAsJsonObject(), objectFieldName );
			if ( jsonValue == null || jsonValue.isJsonNull() ) {
				continue;
			}
			JsonObject jsonObject = jsonValue.getAsJsonObject();
			addDocumentFieldsRecursively( tmp, jsonObject, objectFieldName );
		}

		// Add to the document the additional fields created when indexing the value
		for ( PrimitiveProjection subProjection : primitiveProjections.values() ) {
			subProjection.addDocumentField( tmp, hit, conversionContext );
		}

		return conversionContext.twoWayConversionContext( bridge ).get( absoluteName, tmp );
	}

	public void addDocumentFieldsRecursively(Document tmp, JsonElement value, String fieldName) {
		if ( value == null || value.isJsonNull() ) {
			return;
		}

		PrimitiveProjection configuredProjection = primitiveProjections.get( fieldName );
		if ( configuredProjection != null ) {
			// Those projections are handled separately, see the calling method
			return;
		}

		if ( value.isJsonObject() ) {
			JsonObject jsonObject = value.getAsJsonObject();
			for ( Map.Entry<String, JsonElement> entry : jsonObject.entrySet() ) {
				String nestedFieldName = fieldName + "." + entry.getKey();
				JsonElement nestedFieldValue = entry.getValue();
				addDocumentFieldsRecursively( tmp, nestedFieldValue, nestedFieldName );
			}
		}
		else if ( value.isJsonArray() ) {
			JsonArray jsonArray = value.getAsJsonArray();
			for ( JsonElement nestedValue : jsonArray ) {
				addDocumentFieldsRecursively( tmp, nestedValue, fieldName );
			}
		}
		else {
			JsonPrimitive primitive = value.getAsJsonPrimitive();

			if ( primitive.isBoolean() ) {
				tmp.add( new StringField( fieldName, String.valueOf( primitive.getAsBoolean() ), Store.NO ) );
			}
			else if ( primitive.isNumber() ) {
				tmp.add( new DoubleField( fieldName, primitive.getAsDouble(), Store.NO ) );
			}
			else if ( primitive.isString() ) {
				tmp.add( new StringField( fieldName, primitive.getAsString(), Store.NO ) );
			}
			else {
				// TODO HSEARCH-2255 Better raise an exception?
				tmp.add( new StringField( fieldName, primitive.getAsString(), Store.NO ) );
			}
		}
	}
}
