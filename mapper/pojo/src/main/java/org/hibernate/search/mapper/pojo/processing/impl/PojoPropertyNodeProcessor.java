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
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoElementImpl;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.spi.Closer;

/**
 * @author Yoann Rodiere
 */
class PojoPropertyNodeProcessor<P, T> implements PojoNodeProcessor<P> {

	private final PropertyHandle handle;
	private final Collection<Bridge> bridges;
	private final Collection<PojoNodeProcessor<? super T>> nestedProcessors;

	PojoPropertyNodeProcessor(PropertyHandle handle,
			Collection<Bridge> bridges,
			Collection<FunctionBridgeProcessor<? super T, ?>> functionBridgeProcessors,
			Collection<AbstractPojoNodeProcessorBuilder<? super T>> nestedProcessorBuilders) {
		this.handle = handle;
		this.bridges = bridges.isEmpty() ? Collections.emptyList() : new ArrayList<>( bridges );
		this.nestedProcessors = functionBridgeProcessors.isEmpty() && nestedProcessorBuilders.isEmpty()
				? Collections.emptyList()
				: new ArrayList<>( functionBridgeProcessors.size() + nestedProcessorBuilders.size() );
		this.nestedProcessors.addAll( functionBridgeProcessors );
		nestedProcessorBuilders.forEach( builder -> this.nestedProcessors.add( builder.build() ) );
	}

	@Override
	public final void process(DocumentElement target, P source) {
		// TODO add generic type parameters to property handles
		T propertyValue = (T) handle.get( source );
		if ( !bridges.isEmpty() ) {
			PojoElement bridgedElement = new PojoElementImpl( propertyValue );
			for ( Bridge bridge : bridges ) {
				bridge.write( target, bridgedElement );
			}
		}
		for ( PojoNodeProcessor<? super T> processor : nestedProcessors ) {
			processor.process( target, propertyValue );
		}
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( Bridge::close, bridges );
			closer.pushAll( PojoNodeProcessor::close, nestedProcessors );
		}
	}
}
