/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.ParameterizedBridge;

/**
 * @author Emmanuel Bernard
 */
public class TruncateStringBridge implements StringBridge, ParameterizedBridge {

	private int div;

	public Object stringToObject(String stringValue) {
		return stringValue;
	}

	@Override
	public String objectToString(Object object) {
		String string = (String) object;
		return object != null ? string.substring( 0, string.length() / div ) : null;
	}

	@Override
	public void setParameterValues(Map<String, String> parameters) {
		div = Integer.parseInt( parameters.get( "dividedBy" ) );
	}
}
