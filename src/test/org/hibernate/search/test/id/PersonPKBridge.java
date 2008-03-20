//$
package org.hibernate.search.test.id;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Emmanuel Bernard
 */
public class PersonPKBridge implements TwoWayFieldBridge {

	public Object get(String name, Document document) {
		PersonPK id = new PersonPK();
		Field field = document.getField( name + ".firstName" );
		id.setFirstName( field.stringValue() );
		field = document.getField( name + ".lastName" );
		id.setLastName( field.stringValue() );
		return id;
	}

	public String objectToString(Object object) {
		PersonPK id = (PersonPK) object;
		StringBuilder sb = new StringBuilder();
		sb.append( id.getFirstName() ).append( " " ).append( id.getLastName() );
		return sb.toString();
	}

	public void set(String name, Object value, Document document, Field.Store store, Field.Index index, Field.TermVector termVector, Float boost) {
		PersonPK id = (PersonPK) value;
		Field field = new Field( name + ".firstName", id.getFirstName(), store, index, termVector );
		if ( boost != null ) field.setBoost( boost );
		document.add( field );
		field = new Field( name + ".lastName", id.getLastName(), store, index, termVector );
		if ( boost != null ) field.setBoost( boost );
		document.add( field );
	}
}
