/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContextExtension;

/**
 * A bridge between a POJO property and an element of the index schema.
 * <p>
 * The {@code PropertyBridge} interface is a more powerful version of {@link ValueBridge}
 * that can use reflection to get information about the property being bridged,
 * and can contribute more than one index field, in particular.
 *
 * @param <P> The type of the property on the POJO side of the bridge.
 */
public interface PropertyBridge<P> extends AutoCloseable {

	/**
	 * Write to fields in the given {@link DocumentElement},
	 * using the given {@code bridgedElement} as input and transforming it as necessary.
	 * <p>
	 * Writing to the {@link DocumentElement} should be done using
	 * {@link org.hibernate.search.engine.backend.document.IndexFieldReference}s retrieved
	 * when the bridge was {@link PropertyBinder#bind(PropertyBindingContext) bound}.
	 * <p>
	 * <strong>Warning:</strong> Reading from {@code bridgedElement} should be done with care.
	 * Any read that was not declared during {@link PropertyBinder#bind(PropertyBindingContext) binding}
	 * (by declaring dependencies using {@link PropertyBindingContext#dependencies()}
	 * or (advanced use) creating an accessor using {@link PropertyBindingContext#bridgedElement()})
	 * may lead to out-of-sync indexes,
	 * because Hibernate Search will consider the read property irrelevant to indexing
	 * and will not reindex entities when that property changes.
	 *
	 * @param target The {@link DocumentElement} to write to.
	 * @param bridgedElement The element this bridge is applied to, from which data should be read.
	 * @param context A context that can be
	 * {@link PropertyBridgeWriteContext#extension(PropertyBridgeWriteContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 */
	void write(DocumentElement target, P bridgedElement, PropertyBridgeWriteContext context);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
