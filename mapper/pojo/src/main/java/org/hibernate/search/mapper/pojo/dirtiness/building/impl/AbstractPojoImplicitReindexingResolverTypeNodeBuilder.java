/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

abstract class AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, U>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder<T> {

	private final BoundPojoModelPathTypeNode<U> modelPath;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?>> propertyNodeBuilders =
			new LinkedHashMap<>();

	// Use a LinkedHashSet for deterministic iteration
	private Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexing = new LinkedHashSet<>();

	AbstractPojoImplicitReindexingResolverTypeNodeBuilder(BoundPojoModelPathTypeNode<U> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
	}

	@Override
	BoundPojoModelPathTypeNode<U> getModelPath() {
		return modelPath;
	}

	PojoTypeModel<U> getTypeModel() {
		return modelPath.getTypeModel();
	}

	PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> property(String propertyName) {
		return getOrCreatePropertyBuilder( propertyName );
	}

	void addDirtyPathTriggeringReindexing(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		checkNotFrozen();
		dirtyPathsTriggeringReindexing.add( dirtyPathFromEntityType.toUnboundPath() );
	}

	@Override
	void onFreeze(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingCollector) {
		dirtyPathsTriggeringReindexingCollector.addAll( dirtyPathsTriggeringReindexing );
		for ( PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> builder : propertyNodeBuilders.values() ) {
			builder.freeze();
			dirtyPathsTriggeringReindexingCollector.addAll(
					builder.getDirtyPathsTriggeringReindexingIncludingNestedNodes()
			);
		}
	}

	@Override
	final Optional<PojoImplicitReindexingResolver<T>> doBuild(Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		boolean markForReindexing = !dirtyPathsTriggeringReindexing.isEmpty();

		Collection<PojoImplicitReindexingResolver<? super U>> immutableNestedNodes =
				propertyNodeBuilders.isEmpty() ? Collections.emptyList() : new ArrayList<>( propertyNodeBuilders.size() );
		propertyNodeBuilders.values().stream()
				.map( builder -> builder.build( allPotentialDirtyPaths ) )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedNodes::add );

		if ( !markForReindexing && immutableNestedNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't resolve to anything,
			 * then it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( doBuild( markForReindexing, immutableNestedNodes ) );
		}
	}

	abstract PojoImplicitReindexingResolver<T> doBuild(boolean markForReindexing,
			Collection<PojoImplicitReindexingResolver<? super U>> immutableNestedNodes);

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> getOrCreatePropertyBuilder(String propertyName) {
		return propertyNodeBuilders.computeIfAbsent( propertyName, this::createPropertyBuilder );
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> createPropertyBuilder(String propertyName) {
		checkNotFrozen();
		PropertyHandle handle = modelPath.getTypeModel().getProperty( propertyName ).getHandle();
		return new PojoImplicitReindexingResolverPropertyNodeBuilder<>(
				modelPath.property( handle ), buildingHelper
		);
	}
}
