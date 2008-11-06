//$Id$
package org.hibernate.search.bridge;

/**
 * Transform an object into a string representation.
 * 
 * All implementations are required to be threadsafe;
 * usually this is easily achieved avoiding the usage
 * of class fields, unless they are either immutable
 * or needed to store parameters.
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
