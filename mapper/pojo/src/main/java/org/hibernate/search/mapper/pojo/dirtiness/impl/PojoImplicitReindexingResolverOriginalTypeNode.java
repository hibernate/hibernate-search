/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolver} node working at the type level, without casting.
 * <p>
 * This node may contribute entities to reindex to the collector as well as delegate to
 * {@link PojoImplicitReindexingResolverPropertyNode property nodes} for deeper resolution.
 *
 * @param <T> The type of "dirty" objects received as input.
 */
public class PojoImplicitReindexingResolverOriginalTypeNode<T> extends PojoImplicitReindexingResolver<T> {

	private final boolean markForReindexing;
	private final Collection<PojoImplicitReindexingResolverPropertyNode<? super T, ?>> propertyNodes;

	public PojoImplicitReindexingResolverOriginalTypeNode(boolean markForReindexing,
			Collection<PojoImplicitReindexingResolverPropertyNode<? super T, ?>> propertyNodes) {
		this.markForReindexing = markForReindexing;
		this.propertyNodes = propertyNodes;
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "markForReindexing", markForReindexing );
		builder.startList( "propertyNodes" );
		for ( PojoImplicitReindexingResolverPropertyNode<?, ?> node : propertyNodes ) {
			builder.value( node );
		}
		builder.endList();
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty) {
		if ( markForReindexing ) {
			collector.markForReindexing( dirty );
		}
		for ( PojoImplicitReindexingResolverPropertyNode<? super T, ?> node : propertyNodes ) {
			node.resolveEntitiesToReindex( collector, runtimeIntrospector, dirty );
		}
	}
}
