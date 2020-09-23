/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.OptionalInt;
import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class OptionalIntValueExtractor implements ContainerExtractor<OptionalInt, Integer> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.OPTIONAL_INT;
	}

	@Override
	public void extract(OptionalInt container, Consumer<Integer> consumer) {
		if ( container == null ) {
			return;
		}
		if ( container.isPresent() ) {
			consumer.accept( container.getAsInt() );
		}
	}

	@Override
	public boolean multiValued() {
		return false;
	}
}
