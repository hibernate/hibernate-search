/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import java.util.Map;

/**
 * Allow parameter injection to a given bridge.
 * <p>
 * Implementors need to be threadsafe, but the
 * setParameterValues method doesn't need any
 * guard as initialization is always safe.
 *
 * @author Emmanuel Bernard
 */
public interface ParameterizedBridge {

	/**
	 * Called on the bridge implementation to pass the parameters.
	 *
	 * @param parameters map containing string based parameters to be passed to the parameterized bridge. The map is never
	 * {@code null}.
	 */
	void setParameterValues(Map<String, String> parameters);
}
