/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.util.BitSet;
import java.util.List;

import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinals;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * Information about associations involved in reindexing.
 */
public final class PojoImplicitReindexingAssociationInverseSideResolver implements AutoCloseable, ToStringTreeAppendable {

	private final PojoPathOrdinals pathOrdinals;

	private final PojoPathFilter dirtyContainingAssociationFilter;

	private final List<List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>>> resolversByOrdinal;

	public PojoImplicitReindexingAssociationInverseSideResolver(PojoPathOrdinals pathOrdinals,
			PojoPathFilter dirtyContainingAssociationFilter,
			List<List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>>> resolversByOrdinal) {
		this.pathOrdinals = pathOrdinals;
		this.dirtyContainingAssociationFilter = dirtyContainingAssociationFilter;
		this.resolversByOrdinal = resolversByOrdinal;
	}

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "dirtyContainingAssociationFilter", dirtyContainingAssociationFilter );
		builder.startObject( "resolversByAssociationPath" );
		for ( int i = 0; i < resolversByOrdinal.size(); i++ ) {
			List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>> resolvers = resolversByOrdinal.get( i );
			if ( resolvers != null ) {
				builder.attribute( pathOrdinals.toPath( i ), resolvers );
			}
		}
		builder.endObject();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			for ( List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>> ordinalList : resolversByOrdinal ) {
				closer.pushAll( PojoImplicitReindexingAssociationInverseSideResolverNode::close, ordinalList );
			}
		}
	}

	public PojoPathFilter dirtyContainingAssociationFilter() {
		return dirtyContainingAssociationFilter;
	}

	/**
	 * Adds the inverse side of dirty associations (that should cause reindexing) to {@code collector},
	 * taking into account the given "association states" and the bitset describing the dirtiness of associations.
	 * @param collector A collector for entities pointed to be dirty associations, which should be reindexed.
	 * @param dirtyAssociationPaths The set of dirty paths that involve associations in an entity instance.
	 * @param oldState The old state of the entity whose associations are dirty.
	 * May be {@code null}, in which case this state will not yield any reindexing.
	 * @param newState The new state of the entity whose associations are dirty.
	 * May be {@code null}, in which case this state will not yield any reindexing.
	 * @param context A context related to the entity root
	 */
	public void resolveEntitiesToReindex(PojoReindexingAssociationInverseSideCollector collector,
			BitSet dirtyAssociationPaths, Object[] oldState, Object[] newState,
			PojoImplicitReindexingAssociationInverseSideResolverRootContext context) {
		int resolverSize = resolversByOrdinal.size();
		for ( int i = dirtyAssociationPaths.nextSetBit( 0 ); i >= 0 && i < resolverSize;
				i = dirtyAssociationPaths.nextSetBit( i + 1 ) ) {
			for ( PojoImplicitReindexingAssociationInverseSideResolverNode<Object> resolver : resolversByOrdinal.get( i ) ) {
				if ( oldState != null ) {
					resolver.resolveEntitiesToReindex( collector, oldState[i], context );
				}
				if ( newState != null ) {
					resolver.resolveEntitiesToReindex( collector, newState[i], context );
				}
			}
		}
	}
}
