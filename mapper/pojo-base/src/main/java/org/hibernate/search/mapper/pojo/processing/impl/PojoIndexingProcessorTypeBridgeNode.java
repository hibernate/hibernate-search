/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying a {@link TypeBridge}.
 *
 * @param <T> The processed type.
 */
public class PojoIndexingProcessorTypeBridgeNode<T> extends PojoIndexingProcessor<T> {

	private final BeanHolder<? extends TypeBridge<? super T>> bridgeHolder;

	public PojoIndexingProcessorTypeBridgeNode(BeanHolder<? extends TypeBridge<? super T>> bridgeHolder) {
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( TypeBridge::close, bridgeHolder, BeanHolder::get );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "apply type bridge" );
		appender.attribute( "bridge", bridgeHolder );
	}

	@Override
	public final void process(DocumentElement target, T source, PojoIndexingProcessorRootContext context) {
		bridgeHolder.get().write( target, source, context.sessionContext().typeBridgeWriteContext() );
	}

}
