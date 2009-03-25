//$Id$
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Base class for numbers - integer, double, etc.
 * 
 * @author Emmanuel Bernard
 */
public abstract class NumberBridge implements TwoWayStringBridge {
	public String objectToString(Object object) {
		return object != null ?
				object.toString() :
				null;
	}
}
