/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for extracting the value of a property,
 * and applying nested processor nodes as well as {@link PropertyBridge}s to this value.
 *
 * @param <T> The property holder type
 * @param <P> The property type
 */
public class PojoIndexingProcessorPropertyNode<T, P> extends PojoIndexingProcessor<T> {

	private final ValueReadHandle<P> handle;
	private final Collection<BeanHolder<? extends PropertyBridge>> propertyBridgeHolders;
	private final PojoIndexingProcessor<? super P> nested;

	public PojoIndexingProcessorPropertyNode(ValueReadHandle<P> handle,
			Collection<BeanHolder<? extends PropertyBridge>> propertyBridgeHolders,
			PojoIndexingProcessor<? super P> nested) {
		this.handle = handle;
		this.propertyBridgeHolders = propertyBridgeHolders;
		this.nested = nested;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( holder -> holder.get().close(), propertyBridgeHolders );
			closer.pushAll( BeanHolder::close, propertyBridgeHolders );
			closer.push( PojoIndexingProcessor::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "process property" );
		builder.attribute( "handle", handle );
		builder.startList( "bridges" );
		for ( BeanHolder<? extends PropertyBridge> bridgeHolder : propertyBridgeHolders ) {
			builder.value( bridgeHolder.get() );
		}
		builder.endList();
		builder.attribute( "nested", nested );
	}

	@Override
	public final void process(DocumentElement target, T source, PojoIndexingProcessorSessionContext sessionContext) {
		P propertyValue = handle.get( source );
		for ( BeanHolder<? extends PropertyBridge> bridgeHolder : propertyBridgeHolders ) {
			bridgeHolder.get().write( target, propertyValue, sessionContext.propertyBridgeWriteContext() );
		}
		nested.process( target, propertyValue, sessionContext );
	}
}
