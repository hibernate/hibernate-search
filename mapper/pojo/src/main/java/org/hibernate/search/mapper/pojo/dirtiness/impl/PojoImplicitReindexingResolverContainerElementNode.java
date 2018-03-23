/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolver} node dealing with a specific container type,
 * extracting values from the container then applying nested resolvers to the values.
 *
 * @param <C> The container type received as input, for instance {@code Map<String, Collection<MyEntityType>>}.
 * @param <V> The extracted value type, for instance {@code MyEntityType}.
 */
public class PojoImplicitReindexingResolverContainerElementNode<C, V> extends PojoImplicitReindexingResolver<C> {

	private final ContainerValueExtractor<C, V> extractor;
	private final boolean markForReindexing;
	private final PojoImplicitReindexingResolver<V> valueTypeNode;

	public PojoImplicitReindexingResolverContainerElementNode(ContainerValueExtractor<C, V> extractor,
			boolean markForReindexing, PojoImplicitReindexingResolver<V> valueTypeNode) {
		this.extractor = extractor;
		this.markForReindexing = markForReindexing;
		this.valueTypeNode = valueTypeNode;
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "extractor", extractor );
		builder.attribute( "markForReindexing", markForReindexing );
		builder.attribute( "valueTypeNode", valueTypeNode );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector, C dirty) {
		try ( Stream<V> stream = extractor.extract( dirty ) ) {
			stream.forEach( containerElement -> resolveEntitiesToReindexForContainerElement(
					collector, containerElement
			) );
		}
	}

	private void resolveEntitiesToReindexForContainerElement(PojoReindexingCollector collector, V containerElement) {
		if ( containerElement != null ) {
			if ( markForReindexing ) {
				collector.markForReindexing( containerElement );
			}
			if ( valueTypeNode != null ) {
				valueTypeNode.resolveEntitiesToReindex( collector, containerElement );
			}
		}
	}
}
