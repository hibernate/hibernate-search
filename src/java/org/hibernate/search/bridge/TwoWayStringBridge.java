//$Id$
package org.hibernate.search.bridge;

/**
 * StringBridge allowing a translation from the String back to the Object
 * objectToString( stringToObject( string ) ) and stringToObject( objectToString( object ) )
 * should be "idempotent". More precisely,
 *
 * objectToString( stringToObject( string ) ).equals(string) for string not null
 * stringToObject( objectToString( object ) ).equals(object) for object not null 
 * 
 * As for all Bridges implementors must be threasafe.
 * 
 * @author Emmanuel Bernard
 */
public interface TwoWayStringBridge extends StringBridge {
	/**
	 * Convert the string representation to an object
	 */
	Object stringToObject(String stringValue);
}
