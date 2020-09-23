/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class CollectionElementExtractor<T> implements ContainerExtractor<Collection<T>, T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.COLLECTION;
	}

	@Override
	public void extract(Collection<T> container, Consumer<T> consumer) {
		if ( container == null ) {
			return;
		}
		for ( T element : container ) {
			consumer.accept( element );
		}
	}
}
