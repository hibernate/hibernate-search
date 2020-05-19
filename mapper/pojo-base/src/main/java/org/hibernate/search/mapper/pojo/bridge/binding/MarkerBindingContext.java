/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

public interface MarkerBindingContext extends BindingContext {

	/**
	 * Sets the marker object resulting from this binding.
	 *
	 * @param marker The marker object to attach to the marked property.
	 */
	void marker(Object marker);

	/**
	 * Sets the marker object resulting from this binding.
	 *
	 * @param marker The marker object to attach to the marked property.
	 * @deprecated Use {@link #marker(Object)} instead.
	 */
	@Deprecated
	default void setMarker(Object marker) {
		marker( marker );
	}

}
