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
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.impl.Closer;

abstract class AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, U>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder<T> {

	private final BoundPojoModelPathTypeNode<U> modelPath;

	private final PojoImplicitReindexingResolverMarkingNodeBuilder<U> markingNodeBuilder;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?>> propertyNodeBuilders =
			new LinkedHashMap<>();

	AbstractPojoImplicitReindexingResolverTypeNodeBuilder(BoundPojoModelPathTypeNode<U> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
		this.markingNodeBuilder = new PojoImplicitReindexingResolverMarkingNodeBuilder<>( modelPath, buildingHelper );
	}

	@Override
	BoundPojoModelPathTypeNode<U> getModelPath() {
		return modelPath;
	}

	@Override
	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingResolverMarkingNodeBuilder::closeOnFailure, markingNodeBuilder );
			closer.pushAll(
					AbstractPojoImplicitReindexingResolverNodeBuilder::closeOnFailure,
					propertyNodeBuilders.values()
			);
		}
	}

	PojoTypeModel<U> getTypeModel() {
		return modelPath.getTypeModel();
	}

	PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> property(String propertyName) {
		return getOrCreatePropertyBuilder( propertyName );
	}

	void addDirtyPathTriggeringReindexing(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		checkNotFrozen();
		markingNodeBuilder.addDirtyPathTriggeringReindexing( dirtyPathFromEntityType );
	}

	@Override
	void onFreeze(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingCollector) {
		markingNodeBuilder.freeze();
		dirtyPathsTriggeringReindexingCollector.addAll(
				markingNodeBuilder.getDirtyPathsTriggeringReindexingIncludingNestedNodes()
		);
		for ( PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> builder : propertyNodeBuilders.values() ) {
			builder.freeze();
			dirtyPathsTriggeringReindexingCollector.addAll(
					builder.getDirtyPathsTriggeringReindexingIncludingNestedNodes()
			);
		}
	}

	@Override
	final <S> Optional<PojoImplicitReindexingResolverNode<T, S>> doBuild(PojoPathFilterFactory<S> pathFilterFactory,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		Collection<PojoImplicitReindexingResolverNode<? super U, S>> immutableNestedNodes = new ArrayList<>();
		markingNodeBuilder.build( pathFilterFactory, allPotentialDirtyPaths )
				.ifPresent( immutableNestedNodes::add );
		propertyNodeBuilders.values().stream()
				.map( builder -> builder.build( pathFilterFactory, allPotentialDirtyPaths ) )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedNodes::add );

		if ( immutableNestedNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't delegate to anything, it won't resolve to anything,
			 * thus it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( doBuild( immutableNestedNodes ) );
		}
	}

	abstract <S> PojoImplicitReindexingResolverNode<T, S> doBuild(
			Collection<PojoImplicitReindexingResolverNode<? super U, S>> immutableNestedNodes);

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> getOrCreatePropertyBuilder(String propertyName) {
		return propertyNodeBuilders.computeIfAbsent( propertyName, this::createPropertyBuilder );
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> createPropertyBuilder(String propertyName) {
		checkNotFrozen();
		return new PojoImplicitReindexingResolverPropertyNodeBuilder<>(
				modelPath.property( propertyName ), buildingHelper
		);
	}
}
