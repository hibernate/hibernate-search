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
	 * {@inheritDoc}
	 */
	String objectToString(Object object);
}
