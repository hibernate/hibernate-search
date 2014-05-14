/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.StringBridge;

/**
 * @author Emmanuel Bernard
 */
public class ConcatStringBridge implements StringBridge, ParameterizedBridge {
	public static final String SIZE = "size";
	private int size;

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return "";
		}
		if ( !( object instanceof String ) ) {
			throw new RuntimeException( "not a string" );
		}
		String string = object.toString();
		int maxSize = string.length() >= size ? size : string.length();
		return string.substring( 0, maxSize );
	}

	@Override
	public void setParameterValues(Map<String,String> parameters) {
		size = Integer.valueOf( parameters.get( SIZE ) );
	}
}
