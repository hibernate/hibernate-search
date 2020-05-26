/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying processor property nodes
 * as well as {@link TypeBridge}s to a value.
 *
 * @param <T> The processed type
 */
public class PojoIndexingProcessorTypeNode<T> extends PojoIndexingProcessor<T> {

	private final Iterable<IndexObjectFieldReference> parentIndexObjectReferences;
	private final Collection<BeanHolder<? extends TypeBridge>> bridgeHolders;
	private final Collection<PojoIndexingProcessorPropertyNode<? super T, ?>> propertyNodes;

	public PojoIndexingProcessorTypeNode(Iterable<IndexObjectFieldReference> parentIndexObjectReferences,
			Collection<BeanHolder<? extends TypeBridge>> bridgeHolders,
			Collection<PojoIndexingProcessorPropertyNode<? super T, ?>> propertyNodes) {
		this.parentIndexObjectReferences = parentIndexObjectReferences;
		this.bridgeHolders = bridgeHolders;
		this.propertyNodes = propertyNodes;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( holder -> holder.get().close(), bridgeHolders );
			closer.pushAll( BeanHolder::close, bridgeHolders );
			closer.pushAll( PojoIndexingProcessor::close, propertyNodes );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "process type" );
		builder.attribute( "objectFieldsToCreate", parentIndexObjectReferences );
		builder.startList( "bridges" );
		for ( BeanHolder<? extends TypeBridge> bridgeHolder : bridgeHolders ) {
			builder.value( bridgeHolder.get() );
		}
		builder.endList();
		builder.startList( "propertyNodes" );
		for ( PojoIndexingProcessorPropertyNode<? super T, ?> propertyNode : propertyNodes ) {
			builder.value( propertyNode );
		}
		builder.endList();
	}

	@Override
	@SuppressWarnings("unchecked") // As long as T is not a proxy-specific interface, it will also be implemented by the unproxified object
	public final void process(DocumentElement target, T source, PojoIndexingProcessorSessionContext sessionContext) {
		if ( source == null ) {
			return;
		}
		source = (T) sessionContext.runtimeIntrospector().unproxy( source );
		DocumentElement parentObject = target;
		for ( IndexObjectFieldReference objectFieldReference : parentIndexObjectReferences ) {
			parentObject = parentObject.addObject( objectFieldReference );
		}
		for ( BeanHolder<? extends TypeBridge> bridgeHolder : bridgeHolders ) {
			bridgeHolder.get().write( parentObject, source, sessionContext.typeBridgeWriteContext() );
		}
		for ( PojoIndexingProcessorPropertyNode<? super T, ?> propertyNode : propertyNodes ) {
			// Recursion here
			propertyNode.process( parentObject, source, sessionContext );
		}
	}

}
