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

public class ShortArrayElementExtractor implements ContainerExtractor<short[], Short> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_SHORT;
	}

	@Override
	public void extract(short[] container, Consumer<Short> consumer) {
		if ( container == null ) {
			return;
		}
		for ( short element : container ) {
			consumer.accept( element );
		}
	}
}
