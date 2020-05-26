/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverPropertyNode;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.util.common.impl.Closer;

class PojoImplicitReindexingResolverPropertyNodeBuilder<T, P>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder<T> {

	private final BoundPojoModelPathPropertyNode<T, P> modelPath;
	private final PojoImplicitReindexingResolverValueNodeBuilderDelegate<P> valueWithoutExtractorsBuilderDelegate;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<ContainerExtractorPath, PojoImplicitReindexingResolverContainerElementNodeBuilder<? super P, ?>>
			containerElementNodeBuilders = new LinkedHashMap<>();

	PojoImplicitReindexingResolverPropertyNodeBuilder(BoundPojoModelPathPropertyNode<T, P> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
		BoundContainerExtractorPath<P, P> noExtractorsBoundPath = BoundContainerExtractorPath.noExtractors(
				modelPath.getPropertyModel().typeModel()
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

	@Override
	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingResolverValueNodeBuilderDelegate::closeOnFailure, valueWithoutExtractorsBuilderDelegate );
			closer.pushAll(
					AbstractPojoImplicitReindexingResolverNodeBuilder::closeOnFailure, containerElementNodeBuilders.values()
			);
		}
	}

	PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> value(ContainerExtractorPath extractorPath) {
		if ( !extractorPath.isEmpty() ) {
			PojoImplicitReindexingResolverContainerElementNodeBuilder<? super P, ?> containerElementNodeBuilder =
					containerElementNodeBuilders.get( extractorPath );
			if ( containerElementNodeBuilder == null && !containerElementNodeBuilders.containsKey( extractorPath ) ) {
				checkNotFrozen();
				BoundContainerExtractorPath<P, ?> boundExtractorPath =
						buildingHelper.getExtractorBinder().bindPath(
								modelPath.getPropertyModel().typeModel(), extractorPath
						);
				ContainerExtractorPath explicitExtractorPath = boundExtractorPath.getExtractorPath();
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

	@Override
	protected void onFreeze(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingCollector) {
		valueWithoutExtractorsBuilderDelegate.freeze( dirtyPathsTriggeringReindexingCollector );
		for ( PojoImplicitReindexingResolverContainerElementNodeBuilder<?, ?> builder
				: containerElementNodeBuilders.values() ) {
			if ( builder != null ) { // May happen if the empty container value extractor path was used
				builder.freeze();
				dirtyPathsTriggeringReindexingCollector.addAll(
						builder.getDirtyPathsTriggeringReindexingIncludingNestedNodes()
				);
			}
		}
	}

	@Override
	<S> Optional<PojoImplicitReindexingResolverNode<T, S>> doBuild(PojoPathFilterFactory<S> pathFilterFactory,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		Collection<PojoImplicitReindexingResolverNode<P, S>> valueWithoutExtractorTypeNodes =
				valueWithoutExtractorsBuilderDelegate.buildTypeNodes( pathFilterFactory, allPotentialDirtyPaths );
		Collection<PojoImplicitReindexingResolverNode<? super P, S>> immutableNestedNodes = new ArrayList<>();
		immutableNestedNodes.addAll( valueWithoutExtractorTypeNodes );
		containerElementNodeBuilders.values().stream()
				.distinct() // Necessary because the default extractor path has two possible keys with the same value
				.filter( Objects::nonNull )
				.map( builder -> builder.build( pathFilterFactory, allPotentialDirtyPaths ) )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedNodes::add );

		if ( immutableNestedNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't doesn't have any nested node,
			 * it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverPropertyNode<>(
					modelPath.getValueReadHandle(), createNested( immutableNestedNodes )
			) );
		}
	}

	/*
	 * This generic method is necessary to make it clear to the compiler
	 * that the extracted type and extractor have compatible generic arguments.
	 */
	private <V> PojoImplicitReindexingResolverContainerElementNodeBuilder<? super P, V>
			createContainerBuilder(BoundContainerExtractorPath<P, V> boundExtractorPath) {
		ContainerExtractorHolder<P, V> extractorHolder =
				buildingHelper.createExtractors( boundExtractorPath );
		BoundPojoModelPathValueNode<T, P, V> containerElementPath = modelPath.value( boundExtractorPath );
		return new PojoImplicitReindexingResolverContainerElementNodeBuilder<>(
				containerElementPath, extractorHolder, buildingHelper
		);
	}

}
