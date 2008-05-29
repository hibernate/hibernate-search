//$Id$
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Bridge to use a TwoWayStringBridge as a TwoWayFieldBridge
 *
 * @author Emmanuel Bernard
 */
//TODO use Generics to avoid double declaration of stringBridge 
public class TwoWayString2FieldBridgeAdaptor extends String2FieldBridgeAdaptor implements TwoWayFieldBridge {

	private final TwoWayStringBridge stringBridge;

	public TwoWayString2FieldBridgeAdaptor(TwoWayStringBridge stringBridge) {
		super( stringBridge );
		this.stringBridge = stringBridge;
	}

	public String objectToString(Object object) {
		return stringBridge.objectToString( object );
	}

	public Object get(String name, Document document) {
		Field field = document.getField( name );
		if (field == null) {
			return stringBridge.stringToObject( null );
		}
		else {
			return stringBridge.stringToObject( field.stringValue() );
		}
	}
}
