/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class LongArrayElementExtractor implements ContainerExtractor<long[], Long> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_LONG;
	}

	@Override
	public void extract(long[] container, Consumer<Long> consumer) {
		if ( container == null ) {
			return;
		}
		for ( long element : container ) {
			consumer.accept( element );
		}
	}
}
