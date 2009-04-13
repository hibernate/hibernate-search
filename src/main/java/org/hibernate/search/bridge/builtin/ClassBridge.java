// $Id$
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.SearchException;
import org.hibernate.util.StringHelper;
import org.hibernate.util.ReflectHelper;

/**
 * Convert a Class back and forth
 * 
 * @author Emmanuel Bernard
 */
public class ClassBridge implements TwoWayStringBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		else {
			try {
				return ReflectHelper.classForName( stringValue, ClassBridge.class );
			}
			catch (ClassNotFoundException e) {
				throw new SearchException("Unable to deserialize Class: " + stringValue, e);
			}
		}
	}

	public String objectToString(Object object) {
		return object == null ?
				null :
				( (Class) object).getName();

	}
}
