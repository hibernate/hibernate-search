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

	private static final String FIRST_NAME_SUFFIX = "_content.firstName";
	private static final String LAST_NAME_SUFFIX = "_content.lastName";

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name + FIRST_NAME_SUFFIX, FieldType.STRING )
			.field( name + LAST_NAME_SUFFIX, FieldType.STRING );
	}

	@Override
	public Object get(String name, Document document) {
		PersonPK id = new PersonPK();
		IndexableField field = document.getField( name + FIRST_NAME_SUFFIX );
		id.setFirstName( field.stringValue() );
		field = document.getField( name + LAST_NAME_SUFFIX );
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
		luceneOptions.addFieldToDocument( name + FIRST_NAME_SUFFIX, id.getFirstName(), document );

		luceneOptions.addFieldToDocument( name + LAST_NAME_SUFFIX, id.getLastName(), document );

		//store the unique string representation in the named field
		luceneOptions.addFieldToDocument( name, objectToString( id ), document );
	}

}
