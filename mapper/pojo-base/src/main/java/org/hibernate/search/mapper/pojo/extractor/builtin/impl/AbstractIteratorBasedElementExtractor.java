/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Iterator;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;

abstract class AbstractIteratorBasedElementExtractor<C, T> implements ContainerExtractor<C, T> {
	@Override
	public <T1, C2> void extract(C container, ValueProcessor<T1, ? super T, C2> perValueProcessor, T1 target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		Iterator<T> iterator;
		try {
			iterator = iterator( container );
		}
		catch (RuntimeException e) {
			extractionContext.propagateOrIgnoreContainerExtractionException( e );
			return;
		}
		while ( iterator.hasNext() ) {
			T element;
			try {
				element = iterator.next();
			}
			catch (RuntimeException e) {
				extractionContext.propagateOrIgnoreContainerExtractionException( e );
				// Abort extraction completely:
				// we don't know if hasNext() still works, and we want to avoid infinite loops.
				return;
			}
			perValueProcessor.process( target, element, context, extractionContext );
		}
	}

	protected abstract Iterator<T> iterator(C container);
}
