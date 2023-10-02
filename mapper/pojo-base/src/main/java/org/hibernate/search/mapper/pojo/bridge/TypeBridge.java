/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContextExtension;

/**
 * A bridge between a POJO type and an element of the index schema.
 * <p>
 * The {@code TypeBridge} interface is a more powerful version of {@link ValueBridge}
 * that applies to a whole type instead of a single property,
 * and can contribute more than one index field, in particular.
 *
 * @param <T> The type on the POJO side of the bridge.
 */
public interface TypeBridge<T> extends AutoCloseable {

	/**
	 * Write to fields in the given {@link DocumentElement},
	 * using the given {@code bridgedElement} as input and transforming it as necessary.
	 * <p>
	 * Writing to the {@link DocumentElement} should be done using
	 * {@link org.hibernate.search.engine.backend.document.IndexFieldReference}s retrieved
	 * when the bridge was {@link TypeBinder#bind(TypeBindingContext) bound}.
	 * <p>
	 * <strong>Warning:</strong> Reading from {@code bridgedElement} should be done with care.
	 * Any read that was not declared during {@link TypeBinder#bind(TypeBindingContext) binding}
	 * (by declaring dependencies using {@link TypeBindingContext#dependencies()}
	 * or (advanced use) creating an accessor using {@link TypeBindingContext#bridgedElement()})
	 * may lead to out-of-sync indexes,
	 * because Hibernate Search will consider the read property irrelevant to indexing
	 * and will not reindex entities when that property changes.
	 *
	 * @param target The {@link DocumentElement} to write to.
	 * @param bridgedElement The element this bridge is applied to, from which data should be read.
	 * @param context A context that can be
	 * {@link TypeBridgeWriteContext#extension(TypeBridgeWriteContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 */
	void write(DocumentElement target, T bridgedElement, TypeBridgeWriteContext context);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
