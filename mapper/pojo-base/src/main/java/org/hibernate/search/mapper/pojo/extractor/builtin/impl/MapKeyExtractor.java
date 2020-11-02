/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class MapKeyExtractor<T> implements ContainerExtractor<Map<T, ?>, T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.MAP_KEY;
	}

	@Override
	public <T1, C2> void extract(Map<T, ?> container, ValueProcessor<T1, ? super T, C2> perValueProcessor, T1 target,
			C2 context) {
		if ( container == null ) {
			return;
		}
		for ( T element : container.keySet() ) {
			perValueProcessor.process( target, element, context );
		}
	}
}
