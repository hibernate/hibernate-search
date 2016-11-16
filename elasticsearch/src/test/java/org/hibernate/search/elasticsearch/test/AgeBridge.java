/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * @author Gunnar Morling
 */
public class AgeBridge implements TwoWayFieldBridge, MetadataProvidingFieldBridge {

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.INTEGER );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Integer age = getAge( value );

		if ( age != null ) {
			luceneOptions.addNumericFieldToDocument( name, age, document );
		}
	}

	@Override
	public Object get(String name, Document document) {
		IndexableField field = document.getField( name );
		return field == null ? null : field.numericValue();
	}

	private Integer getAge(Object object) {
		GolfPlayer player = (GolfPlayer) object;

		if ( player.getDateOfBirth() != null ) {
			return Integer.valueOf( 34 );
		}
		else {
			return null;
		}
	}

	@Override
	public String objectToString(Object object) {
		Integer age = getAge(object);
		return age == null ? null : age.toString();
	}
}
