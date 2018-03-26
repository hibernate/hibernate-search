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
import org.hibernate.search.util.spi.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying a {@link ValueBridge} to a value.
 */
public class PojoIndexingProcessorValueBridgeNode<T, R> extends PojoIndexingProcessor<T> {

	private final ValueBridge<? super T, R> bridge;
	private final IndexFieldAccessor<? super R> indexFieldAccessor;

	public PojoIndexingProcessorValueBridgeNode(ValueBridge<? super T, R> bridge,
			IndexFieldAccessor<? super R> indexFieldAccessor) {
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
	public void process(DocumentElement target, T source) {
		R indexFieldValue = bridge.toIndexedValue( source );
		indexFieldAccessor.write( target, indexFieldValue );
	}

}
