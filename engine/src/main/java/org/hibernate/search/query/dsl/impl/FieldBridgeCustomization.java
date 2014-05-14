/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.bridge.FieldBridge;

/**
 * Customization context which allows to inject a field bridge instance to be used for querying the current field.
 * <p>
 * Not part of the public DSL API for the time being in order to gain experiences with using the functionality first;
 * may be made public in a future release by merging with {@code FieldCustomization}.
 *
 * @author Gunnar Morling
 */
public interface FieldBridgeCustomization<T> {

	/**
	 * Sets the field bridge for querying the current field; any other bridge associated with this field will be ignored
	 * for the query
	 *
	 * @param fieldBridge the field bridge to use
	 * @return this object, following the method chaining pattern
	 */
	T withFieldBridge(FieldBridge fieldBridge);
}
