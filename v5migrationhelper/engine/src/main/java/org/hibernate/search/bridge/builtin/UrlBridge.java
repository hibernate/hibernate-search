/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import java.net.URL;
import java.net.MalformedURLException;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;
import org.hibernate.search.exception.SearchException;

/**
 * Bridge for a {@link URL} to a {@link String}.
 *
 * @author Emmanuel Bernard
 */
public class UrlBridge implements TwoWayStringBridge, IgnoreAnalyzerBridge {
	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		else {
			try {
				return new URL( stringValue );
			}
			catch (MalformedURLException e) {
				throw new SearchException( "Unable to build URL: " + stringValue, e );
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
