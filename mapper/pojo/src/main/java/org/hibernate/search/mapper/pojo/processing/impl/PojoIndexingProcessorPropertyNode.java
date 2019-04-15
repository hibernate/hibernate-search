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
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoElementImpl;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for extracting the value of a property,
 * and applying nested processor nodes as well as {@link PropertyBridge}s to this value.
 *
 * @param <T> The property holder type
 * @param <P> The property type
 */
public class PojoIndexingProcessorPropertyNode<T, P> extends PojoIndexingProcessor<T> {

	private final PropertyHandle<P> handle;
	private final Collection<BeanHolder<? extends PropertyBridge>> propertyBridgeHolders;
	private final Collection<PojoIndexingProcessor<? super P>> nestedNodes;

	public PojoIndexingProcessorPropertyNode(PropertyHandle<P> handle,
			Collection<BeanHolder<? extends PropertyBridge>> propertyBridgeHolders,
			Collection<PojoIndexingProcessor<? super P>> nestedNodes) {
		this.handle = handle;
		this.propertyBridgeHolders = propertyBridgeHolders;
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( holder -> holder.get().close(), propertyBridgeHolders );
			closer.pushAll( BeanHolder::close, propertyBridgeHolders );
			closer.pushAll( PojoIndexingProcessor::close, nestedNodes );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "handle", handle );
		builder.startList( "bridges" );
		for ( BeanHolder<? extends PropertyBridge> bridgeHolder : propertyBridgeHolders ) {
			builder.value( bridgeHolder.get() );
		}
		builder.endList();
		builder.startList( "nestedNodes" );
		for ( PojoIndexingProcessor<?> nestedNode : nestedNodes ) {
			builder.value( nestedNode );
		}
		builder.endList();
	}

	@Override
	public final void process(DocumentElement target, T source, AbstractPojoSessionContextImplementor sessionContext) {
		P propertyValue = handle.get( source );
		if ( !propertyBridgeHolders.isEmpty() ) {
			PojoElement bridgedElement = new PojoElementImpl( propertyValue );
			for ( BeanHolder<? extends PropertyBridge> bridgeHolder : propertyBridgeHolders ) {
				bridgeHolder.get().write( target, bridgedElement, sessionContext.getPropertyBridgeWriteContext() );
			}
		}
		for ( PojoIndexingProcessor<? super P> nestedNode : nestedNodes ) {
			nestedNode.process( target, propertyValue, sessionContext );
		}
	}
}
