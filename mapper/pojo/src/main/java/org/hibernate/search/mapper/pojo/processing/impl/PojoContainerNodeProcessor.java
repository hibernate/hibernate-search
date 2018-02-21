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
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractor;
import org.hibernate.search.util.spi.Closer;

/**
 * @author Yoann Rodiere
 */
public class PojoContainerNodeProcessor<C, T> implements PojoNodeProcessor<C> {

	private final ContainerValueExtractor<C, T> extractor;
	private final Collection<PojoNodeProcessor<? super T>> nestedProcessors;

	PojoContainerNodeProcessor(ContainerValueExtractor<C, T> extractor,
			Collection<PojoNodeProcessor<? super T>> nestedProcessors) {
		this.extractor = extractor;
		this.nestedProcessors = nestedProcessors;
	}

	@Override
	public final void process(DocumentElement target, C source) {
		try ( Stream<T> stream = extractor.extract( source ) ) {
			stream.forEach( sourceItem -> processItem( target, sourceItem ) );
		}
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoNodeProcessor::close, nestedProcessors );
		}
	}

	private void processItem(DocumentElement target, T sourceItem) {
		for ( PojoNodeProcessor<? super T> processor : nestedProcessors ) {
			processor.process( target, sourceItem );
		}
	}

}
