/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * @author Emmanuel Bernard
 */
public class PersonPKBridge implements TwoWayFieldBridge, MetadataProvidingFieldBridge {

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name + ".firstName", FieldType.STRING )
			.field( name + ".lastName", FieldType.STRING );
	}

	@Override
	public Object get(String name, Document document) {
		PersonPK id = new PersonPK();
		IndexableField field = document.getField( name + ".firstName" );
		id.setFirstName( field.stringValue() );
		field = document.getField( name + ".lastName" );
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
		luceneOptions.addFieldToDocument( name + ".firstName", id.getFirstName(), document );

		luceneOptions.addFieldToDocument( name + ".lastName", id.getLastName(), document );

		//store the unique string representation in the named field
		luceneOptions.addFieldToDocument( name, objectToString( id ), document );
	}

}
