/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;

/**
 * A binder from a value to a single index field.
 * <p>
 * This binder takes advantage of provided metadata
 * to pick, configure and create a {@link ValueBridge}.
 *
 * @see ValueBridge
 */
public interface ValueBinder {

	/**
	 * Binds a value to an index field.
	 * <p>
	 * The context passed in parameter provides various information about the value being bound.
	 * Implementations are expected to take advantage of that information
	 * and to call one of the {@code bridge(...)} methods on the context
	 * to set the bridge.
	 *
	 * @param context A context object providing information about the value being bound,
	 * and expecting a call to one of its {@code bridge(...)} methods.
	 */
	void bind(ValueBindingContext<?> context);

}
