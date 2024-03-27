/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

public interface MarkerBindingContext extends BindingContext {

	/**
	 * Sets the marker object resulting from this binding.
	 *
	 * @param marker The marker object to attach to the marked property.
	 */
	void marker(Object marker);

}
