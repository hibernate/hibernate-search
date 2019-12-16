/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

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
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "bridge", bridgeHolder );
		builder.attribute( "indexFieldReference", indexFieldReference );
	}

	@Override
	public void process(DocumentElement target, V source, AbstractPojoBackendSessionContext sessionContext) {
		F indexFieldValue = bridgeHolder.get().toIndexedValue( source, sessionContext.getMappingContext().getValueBridgeToIndexedValueContext() );
		target.addValue( indexFieldReference, indexFieldValue );
	}

}
