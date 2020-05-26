/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.util.Collection;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolverNode} dealing with a specific container type,
 * extracting values from the container then applying nested resolvers to the values.
 * <p>
 * This node will only delegate to nested nodes for deeper resolution,
 * and will never contribute entities to reindex directly.
 * At the time of writing, nested nodes are always type nodes,
 * but we might allow other nodes in the future for optimization purposes.
 *
 * @param <C> The container type received as input, for instance {@code Map<String, Collection<MyEntityType>>}.
 * @param <S> The expected type of the object describing the "dirtiness state".
 * @param <V> The extracted value type, for instance {@code MyEntityType}.
 */
public class PojoImplicitReindexingResolverContainerElementNode<C, S, V>
		extends PojoImplicitReindexingResolverNode<C, S> {

	private final ContainerExtractorHolder<C, V> extractorHolder;
	private final Collection<PojoImplicitReindexingResolverNode<V, S>> nestedNodes;

	public PojoImplicitReindexingResolverContainerElementNode(ContainerExtractorHolder<C, V> extractorHolder,
			Collection<PojoImplicitReindexingResolverNode<V, S>> nestedNodes) {
		this.extractorHolder = extractorHolder;
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ContainerExtractorHolder::close, extractorHolder );
			closer.pushAll( PojoImplicitReindexingResolverNode::close, nestedNodes );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "process container element" );
		builder.attribute( "extractor", extractorHolder.get() );
		builder.startList( "nestedNodes" );
		for ( PojoImplicitReindexingResolverNode<?, ?> nestedNode : nestedNodes ) {
			builder.value( nestedNode );
		}
		builder.endList();
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, C dirty, S dirtinessState) {
		try ( Stream<V> stream = extractorHolder.get().extract( dirty ) ) {
			stream.forEach( containerElement -> resolveEntitiesToReindexForContainerElement(
					collector, runtimeIntrospector, containerElement, dirtinessState
			) );
		}
	}

	private void resolveEntitiesToReindexForContainerElement(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, V containerElement, S dirtinessState) {
		if ( containerElement != null ) {
			for ( PojoImplicitReindexingResolverNode<V, S> node : nestedNodes ) {
				node.resolveEntitiesToReindex( collector, runtimeIntrospector, containerElement, dirtinessState );
			}
		}
	}
}
