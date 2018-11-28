/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying a {@link ValueBridge} to a value.
 *
 * @param <V> The processed type
 * @param <F> The index field type
 */
public class PojoIndexingProcessorValueBridgeNode<V, F> extends PojoIndexingProcessor<V> {

	private final ValueBridge<? super V, F> bridge;
	private final IndexFieldAccessor<? super F> indexFieldAccessor;

	public PojoIndexingProcessorValueBridgeNode(ValueBridge<? super V, F> bridge,
			IndexFieldAccessor<? super F> indexFieldAccessor) {
		this.bridge = bridge;
		this.indexFieldAccessor = indexFieldAccessor;
	}

	@Override
	public void close() {
		bridge.close();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "bridge", bridge );
		builder.attribute( "indexFieldAccessor", indexFieldAccessor );
	}

	@Override
	public void process(DocumentElement target, V source, AbstractPojoSessionContextImplementor sessionContext) {
		F indexFieldValue = bridge.toIndexedValue( source, sessionContext.getMappingContext().getToIndexedValueContext() );
		indexFieldAccessor.write( target, indexFieldValue );
	}

}
