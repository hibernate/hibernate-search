/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverCastedTypeNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathCastedTypeNode;

class PojoImplicitReindexingResolverCastedTypeNodeBuilder<T, U>
		extends AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, U> {

	private final BoundPojoModelPathCastedTypeNode<T, U> modelPath;

	PojoImplicitReindexingResolverCastedTypeNodeBuilder(BoundPojoModelPathCastedTypeNode<T, U> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( modelPath, buildingHelper );
		this.modelPath = modelPath;
	}

	@Override
	BoundPojoModelPathCastedTypeNode<T, U> getModelPath() {
		return modelPath;
	}

	@Override
	PojoImplicitReindexingResolverNode<T> doBuild(PojoImplicitReindexingResolverNode<? super U> nestedNode) {
		return new PojoImplicitReindexingResolverCastedTypeNode<>(
				getModelPath().getTypeModel().rawType().caster(), nestedNode
		);
	}
}
