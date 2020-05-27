/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying a {@link TypeBridge}.
 *
 * @param <T> The processed type.
 */
public class PojoIndexingProcessorTypeBridgeNode<T> extends PojoIndexingProcessor<T> {

	private final BeanHolder<? extends TypeBridge> bridgeHolder;

	public PojoIndexingProcessorTypeBridgeNode(BeanHolder<? extends TypeBridge> bridgeHolder) {
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "apply type bridge" );
		builder.attribute( "bridge", bridgeHolder );
	}

	@Override
	public final void process(DocumentElement target, T source, PojoIndexingProcessorSessionContext sessionContext) {
		bridgeHolder.get().write( target, source, sessionContext.typeBridgeWriteContext() );
	}

}
