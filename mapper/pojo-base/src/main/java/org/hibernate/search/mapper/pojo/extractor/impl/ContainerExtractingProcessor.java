/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;

public final class ContainerExtractingProcessor<T, C, V, C2> implements ValueProcessor<T, C, C2> {
	private final ContainerExtractor<? super C, V> extractor;
	private final ValueProcessor<T, ? super V, C2> perValueProcessor;

	public ContainerExtractingProcessor(ContainerExtractor<? super C, V> extractor,
			ValueProcessor<T, ? super V, C2> perValueProcessor) {
		this.extractor = extractor;
		this.perValueProcessor = perValueProcessor;
	}

	@Override
	public String toString() {
		return "ContainerExtractingProcessor["
				+ "extractor=" + extractor
				+ ", perValueProcessor=" + perValueProcessor
				+ "]";
	}

	@Override
	public void process(T target, C container, C2 context) {
		extractor.extract( container, perValueProcessor, target, context );
	}
}
