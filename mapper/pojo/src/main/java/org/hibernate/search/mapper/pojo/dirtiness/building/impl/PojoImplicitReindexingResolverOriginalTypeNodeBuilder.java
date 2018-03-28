/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverOriginalTypeNode;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;

class PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T>
		extends AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, T> {

	PojoImplicitReindexingResolverOriginalTypeNodeBuilder(BoundPojoModelPathTypeNode<T> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( modelPath, buildingHelper );
	}

	@Override
	public Optional<PojoImplicitReindexingResolver<T>> build() {
		Collection<PojoImplicitReindexingResolverPropertyNode<? super T, ?>> immutablePropertyNodes =
				buildPropertyNodes();

		boolean markForReindexing = isMarkForReindexing();

		if ( !markForReindexing && immutablePropertyNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't resolve to anything,
			 * then it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverOriginalTypeNode<>(
					markForReindexing, immutablePropertyNodes
			) );
		}
	}
}
