/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;

/**
 * A binder of property markers.
 * <p>
 * This binder takes advantage of provided metadata
 * to pick, configure and create a marker object.
 */
public interface MarkerBinder {

	/**
	 * Binds a marker to a POJO property.
	 * <p>
	 * Implementations are to call one of the {@code setMarker(...)} methods on the context
	 * to set the marker.
	 *
	 * @param context A context object expecting a call to one of its {@code setMarker(...)} methods.
	 */
	void bind(MarkerBindingContext context);

}
