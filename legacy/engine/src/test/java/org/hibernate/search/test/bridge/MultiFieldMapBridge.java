/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 */
public class MultiFieldMapBridge implements MetadataProvidingFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( ! ( value instanceof Map ) ) {
			throw new IllegalArgumentException( "This field can only be applied on a Map type field" );
		}
		else {
			Map<Object,Object> userValue = (Map) value;
			for ( Map.Entry<Object,Object> e : userValue.entrySet() ) {
				setField( name, String.valueOf( e.getKey() ), String.valueOf( e.getValue() ), document, luceneOptions );
			}
		}
	}

	private void setField(String fieldPrefix, String key, String value, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addFieldToDocument( fieldPrefix + "." + key, value, document );
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.OBJECT );
	}
}
