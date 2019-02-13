/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.util.common.impl.Closer;

public class ContainerExtractorHolder<C, V> implements AutoCloseable {
	private final ContainerExtractor<? super C, V> chain;
	private final List<BeanHolder<?>> chainElementBeanHolders;

	ContainerExtractorHolder(ContainerExtractor<? super C, V> chain,
			List<BeanHolder<?>> chainElementBeanHolders) {
		this.chain = chain;
		this.chainElementBeanHolders = chainElementBeanHolders;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( BeanHolder::close, chainElementBeanHolders );
		}
	}

	public ContainerExtractor<? super C, V> get() {
		return chain;
	}
}
