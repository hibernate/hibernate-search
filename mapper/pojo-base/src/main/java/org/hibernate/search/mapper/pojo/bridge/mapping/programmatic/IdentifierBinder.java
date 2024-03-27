/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;

/**
 * A binder from a POJO property to a document identifier.
 * <p>
 * This binder takes advantage of provided metadata
 * to pick, configure and create a {@link IdentifierBridge}.
 *
 * @see IdentifierBridge
 */
public interface IdentifierBinder {

	/**
	 * Binds a POJO property to a document identifier.
	 * <p>
	 * The context passed in parameter provides various information about the identifier being bound.
	 * Implementations are expected to take advantage of that information
	 * and to call one of the {@code bridge(...)} methods on the context
	 * to set the bridge.
	 *
	 * @param context A context object providing information about the identifier being bound,
	 * and expecting a call to one of its {@code bridge(...)} methods.
	 */
	void bind(IdentifierBindingContext<?> context);

}
