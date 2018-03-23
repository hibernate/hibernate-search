/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverPropertyNode;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.spi.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;

class PojoImplicitReindexingResolverPropertyNodeBuilder<T, P>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder {

	private final BoundPojoModelPathPropertyNode<T, P> modelPath;
	private final PojoImplicitReindexingResolverValueNodeBuilderDelegate<P> valueWithoutExtractorsBuilderDelegate;
	private Map<ContainerValueExtractorPath, PojoImplicitReindexingResolverContainerElementNodeBuilder<? super P, ?>>
			containerElementNodeBuilders = new HashMap<>();

	PojoImplicitReindexingResolverPropertyNodeBuilder(BoundPojoModelPathPropertyNode<T, P> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
		BoundContainerValueExtractorPath<P, P> noExtractorsBoundPath = BoundContainerValueExtractorPath.noExtractors(
				modelPath.getPropertyModel().getTypeModel()
		);
		this.valueWithoutExtractorsBuilderDelegate =
				new PojoImplicitReindexingResolverValueNodeBuilderDelegate<>(
						modelPath.value( noExtractorsBoundPath ), buildingHelper
				);
	}

	@Override
	BoundPojoModelPathPropertyNode<T, P> getModelPath() {
		return modelPath;
	}

	PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> value(ContainerValueExtractorPath extractorPath) {
		if ( !extractorPath.isEmpty() ) {
			PojoImplicitReindexingResolverContainerElementNodeBuilder<? super P, ?> containerElementNodeBuilder =
					containerElementNodeBuilders.get( extractorPath );
			if ( containerElementNodeBuilder == null && !containerElementNodeBuilders.containsKey( extractorPath ) ) {
				BoundContainerValueExtractorPath<P, ?> boundExtractorPath =
						buildingHelper.bindExtractorPath(
								modelPath.getPropertyModel().getTypeModel(), extractorPath
						);
				ContainerValueExtractorPath explicitExtractorPath = boundExtractorPath.getExtractorPath();
				if ( !explicitExtractorPath.isEmpty() ) {
					// Check whether the path was already encountered as an explicit path
					containerElementNodeBuilder = containerElementNodeBuilders.get( explicitExtractorPath );
					if ( containerElementNodeBuilder == null ) {
						containerElementNodeBuilder = createContainerBuilder( boundExtractorPath );
					}
				}
				containerElementNodeBuilders.put( explicitExtractorPath, containerElementNodeBuilder );
				containerElementNodeBuilders.put( extractorPath, containerElementNodeBuilder );
			}
			if ( containerElementNodeBuilder != null ) {
				return containerElementNodeBuilder.value();
			}
		}
		return valueWithoutExtractorsBuilderDelegate;
	}

	Optional<PojoImplicitReindexingResolverPropertyNode<T, P>> build() {
		boolean markForReindexing = valueWithoutExtractorsBuilderDelegate.isMarkForReindexing();
		Optional<PojoImplicitReindexingResolver<P>> valueWithoutExtractorTypeNode =
				valueWithoutExtractorsBuilderDelegate.buildTypeNode();
		Collection<PojoImplicitReindexingResolver<? super P>> immutableNestedNodes = new ArrayList<>();
		valueWithoutExtractorTypeNode.ifPresent( immutableNestedNodes::add );
		containerElementNodeBuilders.values().stream()
				.distinct() // Necessary because the default extractor path has two possible keys with the same value
				.filter( Objects::nonNull )
				.map( PojoImplicitReindexingResolverContainerElementNodeBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedNodes::add );

		if ( !markForReindexing && immutableNestedNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't mark the value for reindexing and doesn't have any nested node,
			 * it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverPropertyNode<>(
					modelPath.getPropertyHandle(), markForReindexing, immutableNestedNodes
			) );
		}
	}

	/*
	 * This generic method is necessary to make it clear to the compiler
	 * that the extracted type and extractor have compatible generic arguments.
	 */
	private <V> PojoImplicitReindexingResolverContainerElementNodeBuilder<? super P, V>
			createContainerBuilder(BoundContainerValueExtractorPath<P, V> boundExtractorPath) {
		ContainerValueExtractor<? super P, V> extractor =
				buildingHelper.createExtractors( boundExtractorPath );
		BoundPojoModelPathValueNode<T, P, V> containerElementPath = modelPath.value( boundExtractorPath );
		return new PojoImplicitReindexingResolverContainerElementNodeBuilder<>(
				containerElementPath, extractor, buildingHelper
		);
	}

}
