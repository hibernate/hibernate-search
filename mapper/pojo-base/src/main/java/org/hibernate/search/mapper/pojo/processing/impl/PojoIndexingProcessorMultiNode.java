/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying multiple processors.
 *
 * @param <T> The processed type
 */
public class PojoIndexingProcessorMultiNode<T> extends PojoIndexingProcessor<T> {

	private final Collection<? extends PojoIndexingProcessor<? super T>> elements;

	public PojoIndexingProcessorMultiNode(Collection<? extends PojoIndexingProcessor<? super T>> elements) {
		this.elements = elements;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoIndexingProcessor::close, elements );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.startList();
		for ( PojoIndexingProcessor<? super T> element : elements ) {
			builder.value( element );
		}
		builder.endList();
	}

	@Override
	public final void process(DocumentElement target, T source, PojoIndexingProcessorSessionContext sessionContext) {
		for ( PojoIndexingProcessor<? super T> element : elements ) {
			// Recursion here
			element.process( target, source, sessionContext );
		}
	}

}
