/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;

public class OptionalValueExtractor<T> implements ContainerExtractor<Optional<T>, T> {
	@Override
	public Stream<T> extract(Optional<T> container) {
		return container == null ? Stream.empty() : container.map( Stream::of ).orElseGet( Stream::empty );
	}

	@Override
	public boolean isMultiValued() {
		return false;
	}
}
