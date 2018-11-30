/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverContainerElementNode;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.util.impl.common.Closer;

class PojoImplicitReindexingResolverContainerElementNodeBuilder<C, V>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder<C> {

	private final BoundPojoModelPathValueNode<?, ? extends C, V> modelPath;
	private final ContainerValueExtractor<C, V> extractor;
	private final PojoImplicitReindexingResolverValueNodeBuilderDelegate<V> valueBuilderDelegate;

	PojoImplicitReindexingResolverContainerElementNodeBuilder(BoundPojoModelPathValueNode<?, ? extends C, V> modelPath,
			ContainerValueExtractor<C, V> extractor,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
		this.extractor = extractor;
		this.valueBuilderDelegate =
				new PojoImplicitReindexingResolverValueNodeBuilderDelegate<>( modelPath, buildingHelper );
	}

	@Override
	BoundPojoModelPathValueNode<?, ? extends C, V> getModelPath() {
		return modelPath;
	}

	@Override
	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			// TODO HSEARCH-3170 release the extractor beans
			closer.push( PojoImplicitReindexingResolverValueNodeBuilderDelegate::closeOnFailure, valueBuilderDelegate );
		}
	}

	PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> value() {
		return valueBuilderDelegate;
	}

	@Override
	void onFreeze(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingCollector) {
		valueBuilderDelegate.freeze( dirtyPathsTriggeringReindexingCollector );
	}

	@Override
	<S> Optional<PojoImplicitReindexingResolverNode<C, S>> doBuild(PojoPathFilterFactory<S> pathFilterFactory,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		Collection<PojoImplicitReindexingResolverNode<V, S>> valueTypeNodes =
				valueBuilderDelegate.buildTypeNodes( pathFilterFactory, allPotentialDirtyPaths );

		if ( valueTypeNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't have any nested node, it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverContainerElementNode<>(
					extractor, valueTypeNodes
			) );
		}
	}
}
