/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverContainerElementNode;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;

class PojoImplicitReindexingResolverContainerElementNodeBuilder<C, V>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder {

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

	PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> value() {
		return valueBuilderDelegate;
	}

	Optional<PojoImplicitReindexingResolverContainerElementNode<C, V>> build() {
		boolean markForReindexing = valueBuilderDelegate.isMarkForReindexing();
		Optional<PojoImplicitReindexingResolver<V>> valueTypeNode =
				valueBuilderDelegate.buildTypeNode();

		if ( !markForReindexing && !valueTypeNode.isPresent() ) {
			/*
			 * If this resolver doesn't mark the value for reindexing and doesn't have any nested node,
			 * it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverContainerElementNode<>(
					extractor, markForReindexing, valueTypeNode.orElse( null )
			) );
		}
	}
}
