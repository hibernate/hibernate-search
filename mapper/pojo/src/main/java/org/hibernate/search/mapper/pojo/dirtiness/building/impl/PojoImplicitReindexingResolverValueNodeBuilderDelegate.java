/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;

class PojoImplicitReindexingResolverValueNodeBuilderDelegate<V> {

	private final BoundPojoModelPathValueNode<?, ?, V> modelPath;
	private final PojoImplicitReindexingResolverBuildingHelper buildingHelper;
	private PojoImplicitReindexingResolverTypeNodeBuilder<V> typeBuilder;
	private boolean markForReindexing = false;

	PojoImplicitReindexingResolverValueNodeBuilderDelegate(BoundPojoModelPathValueNode<?, ?, V> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this.modelPath = modelPath;
		this.buildingHelper = buildingHelper;
	}

	PojoImplicitReindexingResolverTypeNodeBuilder<V> type() {
		if ( typeBuilder == null ) {
			typeBuilder = new PojoImplicitReindexingResolverTypeNodeBuilder<>( modelPath.type(), buildingHelper );
		}
		return typeBuilder;
	}

	void markForReindexing() {
		markForReindexing = true;
	}

	boolean isMarkForReindexing() {
		return markForReindexing;
	}

	Optional<PojoImplicitReindexingResolver<V>> buildTypeNode() {
		if ( typeBuilder == null ) {
			return Optional.empty();
		}
		else {
			return typeBuilder.build();
		}
	}
}
