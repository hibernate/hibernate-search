/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.util.common.impl.Closer;

final class ChainingContainerExtractorHolder<C, U, V> implements ContainerExtractorHolder<C, V> {
	private final ContainerExtractorHolder<C, U> base;
	private final BeanHolder<? extends ContainerExtractor<? super U, V>> chained;

	public ChainingContainerExtractorHolder(ContainerExtractorHolder<C, U> base,
			BeanHolder<? extends ContainerExtractor<? super U, V>> chained) {
		this.base = base;
		this.chained = chained;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "[" );
		appendToString( builder );
		builder.append( "]" );
		return builder.toString();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ContainerExtractorHolder::close, base );
			closer.push( BeanHolder::close, chained );
		}
	}

	@Override
	public <T, C2> ValueProcessor<T, C, C2> wrap(ValueProcessor<T, ? super V, C2> perValueProcessor) {
		return base.wrap( new ContainerExtractingProcessor<>( chained.get(), perValueProcessor ) );
	}

	@Override
	public boolean multiValued() {
		return base.multiValued() || chained.get().multiValued();
	}

	@Override
	public void appendToString(StringBuilder builder) {
		base.appendToString( builder );
		builder.append( ", " );
		builder.append( chained );
	}
}
