// $Id$
package org.hibernate.search.bridge.builtin;

import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge for <code>URI</code>
 *
 * @author Emmanuel Bernard
 */
public class UriBridge implements TwoWayStringBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		else {
			try {
				return new URI( stringValue );
			}
			catch (URISyntaxException e) {
				throw new SearchException( "Unable to build URI: " + stringValue, e );
			}
		}
	}

	public String objectToString(Object object) {
		return object == null ?
				null :
				object.toString();
	}
}