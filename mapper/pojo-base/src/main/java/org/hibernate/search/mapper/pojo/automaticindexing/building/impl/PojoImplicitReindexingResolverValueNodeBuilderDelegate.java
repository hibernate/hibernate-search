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
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoRuntimePathsBuildingHelper;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;

class PojoImplicitReindexingResolverValueNodeBuilderDelegate<V> {

	private final BoundPojoModelPathValueNode<?, ?, V> modelPath;
	private final PojoImplicitReindexingResolverBuildingHelper buildingHelper;

	private PojoImplicitReindexingResolverOriginalTypeNodeBuilder<V> typeNodeBuilder;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeModel<?>, PojoImplicitReindexingResolverCastedTypeNodeBuilder<V, ?>> castedTypeNodeBuilders =
			new LinkedHashMap<>();

	private boolean frozen = false;

	PojoImplicitReindexingResolverValueNodeBuilderDelegate(BoundPojoModelPathValueNode<?, ?, V> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this.modelPath = modelPath;
		this.buildingHelper = buildingHelper;
	}

	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( AbstractPojoImplicitReindexingResolverNodeBuilder::closeOnFailure, typeNodeBuilder );
			closer.pushAll(
					AbstractPojoImplicitReindexingResolverNodeBuilder::closeOnFailure, castedTypeNodeBuilders.values()
			);
		}
	}

	PojoTypeModel<V> getTypeModel() {
		return modelPath.type().getTypeModel();
	}

	<U> AbstractPojoImplicitReindexingResolverTypeNodeBuilder<V, ?> type(PojoRawTypeModel<U> targetTypeModel) {
		PojoRawTypeModel<? super V> valueRawTypeModel = getTypeModel().rawType();
		if ( valueRawTypeModel.isSubTypeOf( targetTypeModel ) ) {
			// No need to cast, we're already satisfying the requirements
			return type();
		}
		else if ( targetTypeModel.isSubTypeOf( valueRawTypeModel ) ) {
			// Need to downcast
			return getOrCreateCastedTypeNodeBuilder( targetTypeModel );
		}
		else {
			/*
			 * Types are incompatible; this problem should have already been detected and reported
			 * by the caller, so we just throw an assertion failure here.
			 */
			throw new AssertionFailure(
					"Error while building the automatic reindexing resolver at path " + modelPath
							+ ": attempt to convert a reindexing resolver builder to an incorrect type; "
							+ " got " + targetTypeModel + ", but a subtype of " + valueRawTypeModel
							+ " was expected."
			);
		}
	}

	PojoImplicitReindexingResolverOriginalTypeNodeBuilder<V> type() {
		if ( typeNodeBuilder == null ) {
			checkNotFrozen();
			typeNodeBuilder = new PojoImplicitReindexingResolverOriginalTypeNodeBuilder<>( modelPath.type(), buildingHelper );
		}
		return typeNodeBuilder;
	}

	/**
	 * Freeze the builder delegate, signaling that no mutating method will be called anymore
	 * and that derived data can be safely computed.
	 */
	void freeze(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingCollector) {
		checkNotFrozen();
		if ( !frozen ) {
			frozen = true;
			if ( typeNodeBuilder != null ) {
				typeNodeBuilder.freeze();
				dirtyPathsTriggeringReindexingCollector.addAll(
						typeNodeBuilder.getDirtyPathsTriggeringReindexingIncludingNestedNodes()
				);
			}
			for ( PojoImplicitReindexingResolverCastedTypeNodeBuilder<?, ?> builder : castedTypeNodeBuilders.values() ) {
				builder.freeze();
				dirtyPathsTriggeringReindexingCollector.addAll(
						builder.getDirtyPathsTriggeringReindexingIncludingNestedNodes()
				);
			}
		}
	}

	Collection<PojoImplicitReindexingResolverNode<V>> buildTypeNodes(PojoRuntimePathsBuildingHelper pathsBuildingHelper,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		Collection<PojoImplicitReindexingResolverNode<V>> immutableTypeNodes = new ArrayList<>();
		if ( typeNodeBuilder != null ) {
			typeNodeBuilder.build( pathsBuildingHelper, allPotentialDirtyPaths )
					.ifPresent( immutableTypeNodes::add );
		}
		castedTypeNodeBuilders.values().stream()
				.map( builder -> builder.build( pathsBuildingHelper, allPotentialDirtyPaths ) )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableTypeNodes::add );

		return immutableTypeNodes;
	}

	private void checkNotFrozen() {
		if ( frozen ) {
			throw new AssertionFailure(
					"A mutating method was called on " + this + " after it was frozen."
			);
		}
	}

	final void checkFrozen() {
		if ( !frozen ) {
			throw new AssertionFailure(
					"A method was called on " + this + " before it was frozen, but a preliminary freeze is required."
			);
		}
	}

	@SuppressWarnings("unchecked") // We know builders have this exact type, by construction
	private <U> PojoImplicitReindexingResolverCastedTypeNodeBuilder<V, ? extends U> getOrCreateCastedTypeNodeBuilder(
			PojoRawTypeModel<U> targetTypeModel) {
		return (PojoImplicitReindexingResolverCastedTypeNodeBuilder<V, ? extends U>) castedTypeNodeBuilders
				.computeIfAbsent( targetTypeModel, this::createCastedTypeNodeBuilder );
	}

	private <U> PojoImplicitReindexingResolverCastedTypeNodeBuilder<V, ? extends U> createCastedTypeNodeBuilder(
			PojoRawTypeModel<U> targetTypeModel) {
		checkNotFrozen();
		return new PojoImplicitReindexingResolverCastedTypeNodeBuilder<>(
				modelPath.type().castTo( targetTypeModel ), buildingHelper
		);
	}

}
