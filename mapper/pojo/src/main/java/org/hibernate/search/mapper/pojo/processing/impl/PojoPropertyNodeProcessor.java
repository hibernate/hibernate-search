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
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoElementImpl;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

/**
 * @author Yoann Rodiere
 */
public class PojoPropertyNodeProcessor {

	private final PropertyHandle handle;
	private final Collection<ValueProcessor> processors;
	private final Collection<PojoTypeNodeProcessor> indexedEmbeddedProcessors;

	public PojoPropertyNodeProcessor(PropertyHandle handle,
			Collection<ValueProcessor> processors,
			Collection<PojoTypeNodeProcessorBuilder> indexedEmbeddedProcessorBuilders) {
		this.handle = handle;
		this.processors = processors.isEmpty() ? Collections.emptyList() : new ArrayList<>( processors );
		this.indexedEmbeddedProcessors = indexedEmbeddedProcessorBuilders.isEmpty() ?
				Collections.emptyList() : new ArrayList<>( indexedEmbeddedProcessorBuilders.size() );
		indexedEmbeddedProcessorBuilders.forEach( builder -> this.indexedEmbeddedProcessors.add( builder.build() ) );
	}

	public final void process(Object source, DocumentElement destination) {
		Object nestedValue = handle.get( source );
		if ( !processors.isEmpty() ) {
			PojoElement bridgedElement = new PojoElementImpl( nestedValue );
			for ( ValueProcessor processor : processors ) {
				processor.process( destination, bridgedElement );
			}
		}
		for ( PojoTypeNodeProcessor processor : indexedEmbeddedProcessors ) {
			processor.process( nestedValue, destination );
		}
	}

}
