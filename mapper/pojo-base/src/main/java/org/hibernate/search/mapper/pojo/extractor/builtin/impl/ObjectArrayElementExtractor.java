/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class ObjectArrayElementExtractor<T> implements ContainerExtractor<T[], T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_OBJECT;
	}

	@Override
	public <T1, C2> void extract(T[] container, ValueProcessor<T1, ? super T, C2> perValueProcessor, T1 target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		for ( T element : container ) {
			perValueProcessor.process( target, element, context, extractionContext );
		}
	}
}
