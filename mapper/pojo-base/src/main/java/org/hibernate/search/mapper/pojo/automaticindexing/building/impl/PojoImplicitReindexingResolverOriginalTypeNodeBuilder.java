/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverOriginalTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;

class PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T>
		extends AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, T> {

	private final BoundPojoModelPathTypeNode<T> modelPath;

	PojoImplicitReindexingResolverOriginalTypeNodeBuilder(BoundPojoModelPathTypeNode<T> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( modelPath, buildingHelper );
		this.modelPath = modelPath;
	}

	@Override
	public BoundPojoModelPathTypeNode<T> getModelPath() {
		return modelPath;
	}

	@Override
	PojoImplicitReindexingResolverNode<T> doBuild(PojoImplicitReindexingResolverNode<? super T> nestedNode) {
		return new PojoImplicitReindexingResolverOriginalTypeNode<>( nestedNode );
	}
}
