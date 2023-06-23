/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A {@link PojoImplicitReindexingAssociationInverseSideResolverNode} dealing with a specific container type,
 * extracting values from the container then applying nested resolvers to the values.
 *
 * @param <C> The container type received as input, for instance {@code Map<String, Collection<MyEntityType>>}.
 * @param <V> The extracted value type, for instance {@code MyEntityType}.
 */
class PojoImplicitReindexingAssociationInverseSideResolverContainerElementNode<C, V>
		extends PojoImplicitReindexingAssociationInverseSideResolverNode<C> {

	private final ContainerExtractorHolder<C, V> extractorHolder;
	private final PojoImplicitReindexingAssociationInverseSideResolverNode<? super V> nested;
	private final ValueProcessor<PojoReindexingAssociationInverseSideCollector,
			? super C,
			PojoImplicitReindexingAssociationInverseSideResolverRootContext> extractingDelegate;

	public PojoImplicitReindexingAssociationInverseSideResolverContainerElementNode(
			ContainerExtractorHolder<C, V> extractorHolder,
			PojoImplicitReindexingAssociationInverseSideResolverNode<? super V> nested) {
		this.extractorHolder = extractorHolder;
		this.nested = nested;
		this.extractingDelegate = extractorHolder.wrap( (collector, value, context, extractionContext) -> {
			if ( value != null ) {
				nested.resolveEntitiesToReindex( collector, value, context );
			}
		} );
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ContainerExtractorHolder::close, extractorHolder );
			closer.pushAll( PojoImplicitReindexingAssociationInverseSideResolverNode::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "process container element" );
		appender.attribute( "extractor", extractorHolder );
		appender.attribute( "nested", nested );
	}

	@Override
	void resolveEntitiesToReindex(PojoReindexingAssociationInverseSideCollector collector, C container,
			PojoImplicitReindexingAssociationInverseSideResolverRootContext context) {
		extractingDelegate.process( collector, container, context, context );
	}

}
