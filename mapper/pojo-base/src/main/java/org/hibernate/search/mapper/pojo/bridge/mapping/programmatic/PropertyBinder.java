/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

/**
 * A binder from a POJO property to index fields.
 * <p>
 * This binder takes advantage of provided metadata
 * to pick, configure and create a {@link PropertyBridge}.
 *
 * @see PropertyBridge
 */
public interface PropertyBinder {

	/**
	 * Binds a property to index fields.
	 * <p>
	 * The context passed in parameter provides various information about the property being bound.
	 * Implementations are expected to take advantage of that information
	 * and to call one of the {@code bridge(...)} methods on the context
	 * to set the bridge.
	 * <p>
	 * Implementations are also expected to declare dependencies, i.e. the properties
	 * that will later be used in the
	 * {@link PropertyBridge#write(DocumentElement, Object, PropertyBridgeWriteContext)} method,
	 * using {@link PropertyBindingContext#dependencies()}.
	 * Failing that, Hibernate Search will not reindex entities properly when an indexed property is modified.
	 *
	 * @param context A context object providing information about the property being bound,
	 * and expecting a call to one of its {@code bridge(...)} methods.
	 */
	void bind(PropertyBindingContext context);

}
