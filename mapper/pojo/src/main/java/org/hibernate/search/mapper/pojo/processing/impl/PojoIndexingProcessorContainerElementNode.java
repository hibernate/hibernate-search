/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for extracting elements from a container
 * and applying nested processor nodes to the elements.
 *
 * @param <C> The container type
 * @param <V> The extracted value type
 */
public class PojoIndexingProcessorContainerElementNode<C, V> extends PojoIndexingProcessor<C> {

	private final ContainerExtractorHolder<C, V> extractorHolder;
	private final Collection<PojoIndexingProcessor<? super V>> nestedNodes;

	public PojoIndexingProcessorContainerElementNode(ContainerExtractorHolder<C, V> extractorHolder,
			Collection<PojoIndexingProcessor<? super V>> nestedNodes) {
		this.extractorHolder = extractorHolder;
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ContainerExtractorHolder::close, extractorHolder );
			closer.pushAll( PojoIndexingProcessor::close, nestedNodes );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "extractor", extractorHolder.get() );
		builder.startList( "nestedNodes" );
		for ( PojoIndexingProcessor<?> nestedNode : nestedNodes ) {
			builder.value( nestedNode );
		}
		builder.endList();
	}

	@Override
	public final void process(DocumentElement target, C source, AbstractPojoBackendSessionContext sessionContext) {
		try ( Stream<V> stream = extractorHolder.get().extract( source ) ) {
			stream.forEach( sourceItem -> processItem( target, sourceItem, sessionContext ) );
		}
	}

	private void processItem(DocumentElement target, V sourceItem, AbstractPojoBackendSessionContext sessionContext) {
		for ( PojoIndexingProcessor<? super V> nestedNode : nestedNodes ) {
			nestedNode.process( target, sourceItem, sessionContext );
		}
	}

}
