/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverOriginalTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;

class PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T>
		extends AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, T> {

	PojoImplicitReindexingResolverOriginalTypeNodeBuilder(BoundPojoModelPathTypeNode<T> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( modelPath, buildingHelper );
	}

	<S> Optional<PojoImplicitReindexingResolverNode<T, S>> build(PojoPathFilterFactory<S> pathFilterFactory) {
		return build( pathFilterFactory, null );
	}

	@Override
	<S> PojoImplicitReindexingResolverNode<T, S> doBuild(
			Collection<PojoImplicitReindexingResolverNode<? super T, S>> immutableNestedNodes) {
		return new PojoImplicitReindexingResolverOriginalTypeNode<>(
				immutableNestedNodes
		);
	}
}
