/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.mapper.pojo.model.path.impl.PojoRuntimePathsBuildingHelper;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.impl.Closer;

abstract class AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, U>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder<T> {

	private final PojoImplicitReindexingResolverMarkingNodeBuilder<U> markingNodeBuilder;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?>> propertyNodeBuilders =
			new LinkedHashMap<>();

	AbstractPojoImplicitReindexingResolverTypeNodeBuilder(BoundPojoModelPathTypeNode<U> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.markingNodeBuilder = new PojoImplicitReindexingResolverMarkingNodeBuilder<>( modelPath, buildingHelper );
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

	@Override
	abstract BoundPojoModelPathTypeNode<U> getModelPath();

	PojoTypeModel<U> getTypeModel() {
		return getModelPath().getTypeModel();
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
	final Optional<PojoImplicitReindexingResolverNode<T>> doBuild(PojoRuntimePathsBuildingHelper pathsBuildingHelper,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		Collection<PojoImplicitReindexingResolverNode<? super U>> immutableNestedNodes = new ArrayList<>();
		markingNodeBuilder.build( pathsBuildingHelper, allPotentialDirtyPaths )
				.ifPresent( immutableNestedNodes::add );
		propertyNodeBuilders.values().stream()
				.map( builder -> builder.build( pathsBuildingHelper, allPotentialDirtyPaths ) )
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
			return Optional.of( doBuild( createNested( immutableNestedNodes ) ) );
		}
	}

	abstract PojoImplicitReindexingResolverNode<T> doBuild(
			PojoImplicitReindexingResolverNode<? super U> nestedNode);

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> getOrCreatePropertyBuilder(String propertyName) {
		return propertyNodeBuilders.computeIfAbsent( propertyName, this::createPropertyBuilder );
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> createPropertyBuilder(String propertyName) {
		checkNotFrozen();
		return new PojoImplicitReindexingResolverPropertyNodeBuilder<>(
				getModelPath().property( propertyName ), buildingHelper
		);
	}
}
