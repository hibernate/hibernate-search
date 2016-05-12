/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id.withmeta;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * @author Davide D'Alto
 */
public class PersonPKMetadataProviderBridge implements MetadataProvidingFieldBridge, TwoWayFieldBridge {

	@Override
	public Object get(String name, Document document) {
		PersonPK id = new PersonPK();
		IndexableField field = document.getField( firstName( name ) );
		id.setFirstName( field.stringValue() );
		field = document.getField( lastName( name ) );
		id.setLastName( field.stringValue() );
		return id;
	}

	@Override
	public String objectToString(Object object) {
		PersonPK id = (PersonPK) object;
		StringBuilder sb = new StringBuilder();
		sb.append( id.getFirstName() ).append( " " ).append( id.getLastName() );
		return sb.toString();
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		PersonPK id = (PersonPK) value;

		//store each property in a unique field
		luceneOptions.addFieldToDocument( firstName( name ), id.getFirstName(), document );

		luceneOptions.addFieldToDocument( lastName( name ), id.getLastName(), document );

		//store the unique string representation in the named field
		luceneOptions.addFieldToDocument( name, objectToString( id ), document );

	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder
			.field( firstName( name ), FieldType.STRING )
			.field( lastName( name ), FieldType.STRING );
	}

	private static String lastName(String name) {
		return name + "_lastName";
	}

	private static String firstName(String name) {
		return name + "_firstName";
	}
}
