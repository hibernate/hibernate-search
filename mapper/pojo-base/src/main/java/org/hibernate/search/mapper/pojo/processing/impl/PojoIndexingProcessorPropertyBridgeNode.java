/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying a {@link PropertyBridge}.
 *
 * @param <P> The type of the processed property.
 */
public class PojoIndexingProcessorPropertyBridgeNode<P> extends PojoIndexingProcessor<P> {

	private final BeanHolder<? extends PropertyBridge<? super P>> bridgeHolder;

	public PojoIndexingProcessorPropertyBridgeNode(BeanHolder<? extends PropertyBridge<? super P>> bridgeHolder) {
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PropertyBridge::close, bridgeHolder, BeanHolder::get );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "apply property bridge" );
		appender.attribute( "bridge", bridgeHolder );
	}

	@Override
	public final void process(DocumentElement target, P source, PojoIndexingProcessorRootContext context) {
		bridgeHolder.get().write( target, source, context.sessionContext().propertyBridgeWriteContext() );
	}

}
