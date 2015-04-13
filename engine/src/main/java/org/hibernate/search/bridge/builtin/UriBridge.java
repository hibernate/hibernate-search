/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge a {@link URI} to a {@link String}.
 *
 * @author Emmanuel Bernard
 */
public class UriBridge implements TwoWayStringBridge {
	@Override
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

	@Override
	public String objectToString(Object object) {
		return object == null ?
				null :
				object.toString();
	}
}
