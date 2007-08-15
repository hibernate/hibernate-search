//$Id$
package org.hibernate.search.bridge;

/**
 * Transform an object into a string representation
 *
 * @author Emmanuel Bernard
 */
public interface StringBridge {
	
	/**
	 * convert the object representation to a String
	 * The return String must not be null, it can be empty though
	 */
	String objectToString(Object object);
}
