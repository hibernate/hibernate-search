/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverContainerElementNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoRuntimePathsBuildingHelper;
import org.hibernate.search.util.common.impl.Closer;

class PojoImplicitReindexingResolverContainerElementNodeBuilder<C, V>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder<C> {

	private final BoundPojoModelPathValueNode<?, ? extends C, V> modelPath;
	private final ContainerExtractorHolder<C, V> extractorHolder;
	private final PojoImplicitReindexingResolverValueNodeBuilderDelegate<V> valueBuilderDelegate;

	PojoImplicitReindexingResolverContainerElementNodeBuilder(BoundPojoModelPathValueNode<?, ? extends C, V> modelPath,
			ContainerExtractorHolder<C, V> extractorHolder,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
		this.extractorHolder = extractorHolder;
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
			closer.push( ContainerExtractorHolder::close, extractorHolder );
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
	Optional<PojoImplicitReindexingResolverNode<C>> doBuild(PojoRuntimePathsBuildingHelper pathsBuildingHelper,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		Collection<PojoImplicitReindexingResolverNode<V>> valueTypeNodes =
				valueBuilderDelegate.buildTypeNodes( pathsBuildingHelper, allPotentialDirtyPaths );

		if ( valueTypeNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't have any nested node, it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverContainerElementNode<>(
					extractorHolder, createNested( valueTypeNodes )
			) );
		}
	}
}
