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
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

class PojoImplicitReindexingResolverTypeNodeBuilder<T>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder {

	private final BoundPojoModelPathTypeNode<T> modelPath;
	private final Map<String, PojoImplicitReindexingResolverPropertyNodeBuilder<T, ?>> propertyNodeBuilders =
			new HashMap<>();

	PojoImplicitReindexingResolverTypeNodeBuilder(BoundPojoModelPathTypeNode<T> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
	}

	PojoTypeModel<?> getType() {
		return modelPath.getTypeModel();
	}

	PojoImplicitReindexingResolverPropertyNodeBuilder<T, ?> property(String propertyName) {
		return getOrCreatePropertyBuilder( propertyName );
	}

	@Override
	BoundPojoModelPathTypeNode<T> getModelPath() {
		return modelPath;
	}

	public Optional<PojoImplicitReindexingResolver<T>> build() {
		Collection<PojoImplicitReindexingResolverPropertyNode<? super T, ?>> immutablePropertyNodes =
				propertyNodeBuilders.isEmpty() ? Collections.emptyList() : new ArrayList<>( propertyNodeBuilders.size() );
		propertyNodeBuilders.values().stream()
				.map( PojoImplicitReindexingResolverPropertyNodeBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutablePropertyNodes::add );

		if ( immutablePropertyNodes.isEmpty() ) {
			/*
			 * If this resolver doesn't resolve to anything,
			 * then it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverTypeNode<>( immutablePropertyNodes ) );
		}
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<T, ?> getOrCreatePropertyBuilder(String propertyName) {
		return propertyNodeBuilders.computeIfAbsent( propertyName, this::createPropertyBuilder );
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<T, ?> createPropertyBuilder(String propertyName) {
		PropertyHandle handle = modelPath.getTypeModel().getProperty( propertyName ).getHandle();
		return new PojoImplicitReindexingResolverPropertyNodeBuilder<>(
				modelPath.property( handle ), buildingHelper
		);
	}

}
