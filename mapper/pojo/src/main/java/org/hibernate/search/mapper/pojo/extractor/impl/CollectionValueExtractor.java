/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.Collection;
import java.util.stream.Stream;

public class CollectionValueExtractor<T> implements ContainerValueExtractor<Collection<T>, T> {
	private static final CollectionValueExtractor<?> INSTANCE = new CollectionValueExtractor();

	@SuppressWarnings( "unchecked" ) // INSTANCE works for any T
	public static <T> CollectionValueExtractor<T> get() {
		return (CollectionValueExtractor<T>) INSTANCE;
	}

	@Override
	public Stream<T> extract(Collection<T> container) {
		return container == null ? Stream.empty() : container.stream();
	}
}
