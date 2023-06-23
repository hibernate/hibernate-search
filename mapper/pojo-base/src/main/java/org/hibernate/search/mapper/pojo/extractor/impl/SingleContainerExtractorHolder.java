/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;

final class SingleContainerExtractorHolder<C, V> implements ContainerExtractorHolder<C, V> {

	private final BeanHolder<? extends ContainerExtractor<? super C, V>> extractorBeanHolder;

	SingleContainerExtractorHolder(BeanHolder<? extends ContainerExtractor<? super C, V>> extractorBeanHolder) {
		this.extractorBeanHolder = extractorBeanHolder;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		appendToString( builder );
		return builder.toString();
	}

	@Override
	public void close() {
		extractorBeanHolder.close();
	}

	@Override
	public <T, C2> ValueProcessor<T, C, C2> wrap(ValueProcessor<T, ? super V, C2> perValueProcessor) {
		return new ContainerExtractingProcessor<>( extractorBeanHolder.get(), perValueProcessor );
	}

	@Override
	public boolean multiValued() {
		return extractorBeanHolder.get().multiValued();
	}

	@Override
	public void appendToString(StringBuilder builder) {
		builder.append( extractorBeanHolder.get() );
	}
}
