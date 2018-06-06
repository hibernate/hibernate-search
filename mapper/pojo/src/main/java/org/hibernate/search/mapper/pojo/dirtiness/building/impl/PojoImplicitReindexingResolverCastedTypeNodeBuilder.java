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
	PojoImplicitReindexingResolver<T> doBuild(boolean markForReindexing,
			Collection<PojoImplicitReindexingResolver<? super U>> immutableNestedNodes) {
		return new PojoImplicitReindexingResolverCastedTypeNode<>(
				getTypeModel().getRawType().getCaster(), markForReindexing, immutableNestedNodes
		);
	}
}
