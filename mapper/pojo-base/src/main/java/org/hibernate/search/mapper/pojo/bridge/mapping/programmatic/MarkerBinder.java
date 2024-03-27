/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * Implementations are to call one of the {@code marker(...)} methods on the context
	 * to set the marker.
	 *
	 * @param context A context object expecting a call to one of its {@code marker(...)} methods.
	 */
	void bind(MarkerBindingContext context);

}
