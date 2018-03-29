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
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for extracting elements from a container
 * and applying nested processor nodes to the elements.
 */
public class PojoIndexingProcessorContainerElementNode<C, T> extends PojoIndexingProcessor<C> {

	private final ContainerValueExtractor<C, T> extractor;
	private final Collection<PojoIndexingProcessor<? super T>> nestedNodes;

	public PojoIndexingProcessorContainerElementNode(ContainerValueExtractor<C, T> extractor,
			Collection<PojoIndexingProcessor<? super T>> nestedNodes) {
		this.extractor = extractor;
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoIndexingProcessor::close, nestedNodes );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "extractor", extractor );
		builder.startList( "nestedNodes" );
		for ( PojoIndexingProcessor<? super T> nestedNode : nestedNodes ) {
			builder.value( nestedNode );
		}
		builder.endList();
	}

	@Override
	public final void process(DocumentElement target, C source) {
		try ( Stream<T> stream = extractor.extract( source ) ) {
			stream.forEach( sourceItem -> processItem( target, sourceItem ) );
		}
	}

	private void processItem(DocumentElement target, T sourceItem) {
		for ( PojoIndexingProcessor<? super T> nestedNode : nestedNodes ) {
			nestedNode.process( target, sourceItem );
		}
	}

}
