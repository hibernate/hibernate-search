/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Iterator;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class IterableElementExtractor<T> extends AbstractIteratorBasedElementExtractor<Iterable<T>, T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ITERABLE;
	}

	@Override
	protected Iterator<T> iterator(Iterable<T> container) {
		return container.iterator();
	}
}
