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
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoElementImpl;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeNodeProcessor {

	private final Iterable<IndexObjectFieldAccessor> parentObjectAccessors;
	private final Collection<ValueProcessor> typeScopedProcessors;
	private final Collection<PojoPropertyNodeProcessor> propertyScopedProcessors;

	public PojoTypeNodeProcessor(Iterable<IndexObjectFieldAccessor> parentObjectAccessors,
			Collection<ValueProcessor> typeScopedProcessors,
			Collection<PojoPropertyNodeProcessorBuilder> propertyScopedProcessorBuilders) {
		this.parentObjectAccessors = parentObjectAccessors;
		this.typeScopedProcessors = typeScopedProcessors.isEmpty() ? Collections.emptyList() : new ArrayList<>( typeScopedProcessors );
		this.propertyScopedProcessors = propertyScopedProcessorBuilders.isEmpty() ?
				Collections.emptyList() : new ArrayList<>( propertyScopedProcessorBuilders.size() );
		propertyScopedProcessorBuilders.forEach( builder -> this.propertyScopedProcessors.add( builder.build() ) );
	}

	public final void process(Object source, DocumentElement destination) {
		if ( source == null ) {
			return;
		}
		DocumentElement parentObject = destination;
		for ( IndexObjectFieldAccessor objectAccessor : parentObjectAccessors ) {
			parentObject = objectAccessor.add( parentObject );
		}
		if ( !typeScopedProcessors.isEmpty() ) {
			PojoElement bridgedElement = new PojoElementImpl( source );
			for ( ValueProcessor processor : typeScopedProcessors ) {
				processor.process( parentObject, bridgedElement );
			}
		}
		for ( PojoPropertyNodeProcessor processor : propertyScopedProcessors ) {
			// Recursion here
			processor.process( source, parentObject );
		}
	}

}
