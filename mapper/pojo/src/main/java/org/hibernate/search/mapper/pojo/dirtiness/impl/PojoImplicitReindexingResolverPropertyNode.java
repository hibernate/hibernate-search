/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

/**
 * A {@link PojoImplicitReindexingResolver} node dealing with a specific property of a specific type,
 * getting the value from that property then applying nested resolvers to that value.
 * <p>
 * This node will only delegate to nested nodes for deeper resolution,
 * and will never contribute entities to reindex directly.
 * At the time of writing, nested nodes are either type nodes or container element nodes,
 * but we might allow other nodes in the future for optimization purposes.
 *
 * @param <T> The property holder type received as input.
 * @param <P> The property type.
 */
public class PojoImplicitReindexingResolverPropertyNode<T, P> extends PojoImplicitReindexingResolver<T> {

	private final PropertyHandle handle;
	private final Collection<PojoImplicitReindexingResolver<? super P>> nestedNodes;

	public PojoImplicitReindexingResolverPropertyNode(PropertyHandle handle,
			Collection<PojoImplicitReindexingResolver<? super P>> nestedNodes) {
		this.handle = handle;
		this.nestedNodes = nestedNodes;
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "class", getClass().getSimpleName() );
		builder.attribute( "handle", handle );
		builder.startList( "nestedNodes" );
		for ( PojoImplicitReindexingResolver<?> nestedNode : nestedNodes ) {
			builder.value( nestedNode );
		}
		builder.endList();
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoRuntimeIntrospector runtimeIntrospector, T dirty, PojoDirtinessState dirtinessState) {
		// TODO add generic type parameters to property handles
		P propertyValue = (P) handle.get( dirty );
		if ( propertyValue != null ) {
			for ( PojoImplicitReindexingResolver<? super P> node : nestedNodes ) {
				node.resolveEntitiesToReindex( collector, runtimeIntrospector, propertyValue, dirtinessState );
			}
		}
	}
}
