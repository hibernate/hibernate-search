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
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverCastedTypeNode;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathCastedTypeNode;

class PojoImplicitReindexingResolverCastedTypeNodeBuilder<T, U>
		extends AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, U> {

	PojoImplicitReindexingResolverCastedTypeNodeBuilder(BoundPojoModelPathCastedTypeNode<T, U> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( modelPath, buildingHelper );
	}

	@Override
	public Optional<PojoImplicitReindexingResolver<T>> build() {
		Collection<PojoImplicitReindexingResolverPropertyNode<? super U, ?>> immutablePropertyNodes =
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
			return Optional.of( new PojoImplicitReindexingResolverCastedTypeNode<>(
					getTypeModel().getRawType().getCaster(), markForReindexing, immutablePropertyNodes
			) );
		}
	}

}
