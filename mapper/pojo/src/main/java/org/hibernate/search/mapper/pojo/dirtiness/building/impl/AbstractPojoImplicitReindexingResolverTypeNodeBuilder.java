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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

abstract class AbstractPojoImplicitReindexingResolverTypeNodeBuilder<T, U>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder {

	private final BoundPojoModelPathTypeNode<U> modelPath;
	private final Map<String, PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?>> propertyNodeBuilders =
			new HashMap<>();

	private boolean shouldMarkForReindexing = false;

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

	void setShouldMarkForReindexing() {
		shouldMarkForReindexing = true;
	}

	abstract Optional<PojoImplicitReindexingResolver<T>> build();

	final boolean isShouldMarkForReindexing() {
		return shouldMarkForReindexing;
	}

	final Collection<PojoImplicitReindexingResolverPropertyNode<? super U, ?>> buildPropertyNodes() {
		Collection<PojoImplicitReindexingResolverPropertyNode<? super U, ?>> immutablePropertyNodes =
				propertyNodeBuilders.isEmpty() ? Collections.emptyList() : new ArrayList<>( propertyNodeBuilders.size() );
		propertyNodeBuilders.values().stream()
				.map( PojoImplicitReindexingResolverPropertyNodeBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutablePropertyNodes::add );
		return immutablePropertyNodes;
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> getOrCreatePropertyBuilder(String propertyName) {
		return propertyNodeBuilders.computeIfAbsent( propertyName, this::createPropertyBuilder );
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<U, ?> createPropertyBuilder(String propertyName) {
				PropertyHandle handle = modelPath.getTypeModel().getProperty( propertyName ).getHandle();
		return new PojoImplicitReindexingResolverPropertyNodeBuilder<>(
				modelPath.property( handle ), buildingHelper
		);
	}
}
