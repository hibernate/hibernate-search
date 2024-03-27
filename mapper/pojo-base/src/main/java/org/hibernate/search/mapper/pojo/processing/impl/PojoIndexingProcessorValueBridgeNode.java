/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying a {@link ValueBridge} to a value.
 *
 * @param <V> The processed type
 * @param <F> The index field type
 */
public class PojoIndexingProcessorValueBridgeNode<V, F> extends PojoIndexingProcessor<V> {

	private final BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder;
	private final IndexFieldReference<F> indexFieldReference;

	public PojoIndexingProcessorValueBridgeNode(BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder,
			IndexFieldReference<F> indexFieldReference) {
		this.bridgeHolder = bridgeHolder;
		this.indexFieldReference = indexFieldReference;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ValueBridge::close, bridgeHolder, BeanHolder::get );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "apply value bridge" );
		appender.attribute( "bridge", bridgeHolder );
		appender.attribute( "indexField", indexFieldReference );
	}

	@Override
	public void process(DocumentElement target, V source, PojoIndexingProcessorRootContext context) {
		F indexFieldValue = bridgeHolder.get().toIndexedValue( source,
				context.sessionContext().mappingContext().valueBridgeToIndexedValueContext() );
		target.addValue( indexFieldReference, indexFieldValue );
	}

}
