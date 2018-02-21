/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoElementImpl;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.spi.Closer;

/**
 * @author Yoann Rodiere
 */
class PojoPropertyNodeProcessor<P, T> implements PojoNodeProcessor<P> {

	private final PropertyHandle handle;
	private final Collection<PropertyBridge> propertyBridges;
	private final Collection<PojoNodeProcessor<? super T>> nestedProcessors;

	PojoPropertyNodeProcessor(PropertyHandle handle,
			Collection<PropertyBridge> propertyBridges,
			Collection<PojoNodeProcessor<? super T>> nestedProcessors) {
		this.handle = handle;
		this.propertyBridges = propertyBridges;
		this.nestedProcessors = nestedProcessors;
	}

	@Override
	public final void process(DocumentElement target, P source) {
		// TODO add generic type parameters to property handles
		T propertyValue = (T) handle.get( source );
		if ( !propertyBridges.isEmpty() ) {
			PojoElement bridgedElement = new PojoElementImpl( propertyValue );
			for ( PropertyBridge bridge : propertyBridges ) {
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
			closer.pushAll( PropertyBridge::close, propertyBridges );
			closer.pushAll( PojoNodeProcessor::close, nestedProcessors );
		}
	}
}
