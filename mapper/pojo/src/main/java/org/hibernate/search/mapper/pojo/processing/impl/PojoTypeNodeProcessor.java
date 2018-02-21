/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoElementImpl;
import org.hibernate.search.util.spi.Closer;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeNodeProcessor<T> implements PojoNodeProcessor<T> {

	private final Iterable<IndexObjectFieldAccessor> parentObjectAccessors;
	private final Collection<TypeBridge> bridges;
	private final Collection<PojoPropertyNodeProcessor<? super T, ?>> propertyProcessors;

	PojoTypeNodeProcessor(Iterable<IndexObjectFieldAccessor> parentObjectAccessors,
			Collection<TypeBridge> bridges,
			Collection<PojoPropertyNodeProcessorBuilder<? super T, ?>> propertyProcessorBuilders) {
		this.parentObjectAccessors = parentObjectAccessors;
		this.bridges = bridges.isEmpty() ? Collections.emptyList() : new ArrayList<>( bridges );
		this.propertyProcessors = propertyProcessorBuilders.isEmpty() ?
				Collections.emptyList() : new ArrayList<>( propertyProcessorBuilders.size() );
		propertyProcessorBuilders.forEach( builder -> this.propertyProcessors.add( builder.build() ) );
	}

	@Override
	public final void process(DocumentElement target, T source) {
		if ( source == null ) {
			return;
		}
		DocumentElement parentObject = target;
		for ( IndexObjectFieldAccessor objectAccessor : parentObjectAccessors ) {
			parentObject = objectAccessor.add( parentObject );
		}
		if ( !bridges.isEmpty() ) {
			PojoElement bridgedElement = new PojoElementImpl( source );
			for ( TypeBridge bridge : bridges ) {
				bridge.write( parentObject, bridgedElement );
			}
		}
		for ( PojoPropertyNodeProcessor<? super T, ?> processor : propertyProcessors ) {
			// Recursion here
			processor.process( parentObject, source );
		}
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( TypeBridge::close, bridges );
			closer.pushAll( PojoNodeProcessor::close, propertyProcessors );
		}
	}

}
