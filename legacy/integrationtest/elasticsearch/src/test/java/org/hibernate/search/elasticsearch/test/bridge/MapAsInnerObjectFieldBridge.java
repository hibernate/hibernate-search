/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.bridge;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldMetadataCreationContext;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.elasticsearch.bridge.spi.Elasticsearch;
import org.hibernate.search.elasticsearch.cfg.DynamicType;

/**
 * @author Davide D'Alto
 */
public class MapAsInnerObjectFieldBridge implements ParameterizedBridge, MetadataProvidingFieldBridge, TwoWayFieldBridge {

	public static final String DYNAMIC = "dynamicMapping";

	public DynamicType dynamicType = null;

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( !( value instanceof Map ) ) {
			throw new IllegalArgumentException( "This field can only be applied on a Map type field" );
		}
		else {
			Map<Object, Object> userValue = (Map) value;
			for ( Map.Entry<Object, Object> e : userValue.entrySet() ) {
				setField( name, String.valueOf( e.getKey() ), String.valueOf( e.getValue() ), document, luceneOptions );
			}
		}
	}

	private void setField(String fieldPrefix, String key, String value, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addFieldToDocument( fieldPrefix + "." + key, value, document );
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		FieldMetadataCreationContext field = builder.field( name, FieldType.OBJECT );
		if ( dynamicType != null ) {
			field.mappedOn( Elasticsearch.class )
					.dynamic( dynamicType );
		}
	}

	@Override
	public void setParameterValues(Map<String, String> parameters) {
		String dynamicTypeAsString = parameters.get( DYNAMIC );
		if ( dynamicTypeAsString != null ) {
			dynamicType = DynamicType.valueOf( dynamicTypeAsString.toUpperCase( Locale.ROOT ) );
		}
	}

	@Override
	public Object get(String fieldPrefix, Document document) {
		String prefix = fieldPrefix + ".";
		Map<String, String> map = new LinkedHashMap<>();
		document.getFields().stream()
				.filter( f -> f.name().startsWith( prefix ) )
				.forEach( f -> map.put( f.name().substring( prefix.length() ), f.stringValue() ) );
		return map;
	}

	@Override
	public String objectToString(Object object) {
		throw new UnsupportedOperationException();
	}
}
