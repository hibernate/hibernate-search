/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverImpl;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathWalker;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoRuntimePathsBuildingHelper;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.AssertionFailure;

class PojoImplicitReindexingResolverBuilder<T> {

	static Walker walker() {
		return Walker.INSTANCE;
	}

	private final PojoRawTypeModel<T> rawTypeModel;
	private final PojoImplicitReindexingResolverBuildingHelper buildingHelper;

	// Use a LinkedHashSet for deterministic iteration
	private final Set<PojoModelPathValueNode> dirtyPathsTriggeringSelfReindexing = new LinkedHashSet<>();

	private final Map<PojoModelPathValueNode, Map<PojoRawTypeModel<?>, PojoModelPathValueNode>> containingAssociationPaths =
			new LinkedHashMap<>();

	private final PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T> containingEntitiesResolverRootBuilder;

	private boolean frozen = false;

	PojoImplicitReindexingResolverBuilder(PojoRawTypeModel<T> rawTypeModel,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this.rawTypeModel = rawTypeModel;
		this.buildingHelper = buildingHelper;
		this.containingEntitiesResolverRootBuilder = new PojoImplicitReindexingResolverOriginalTypeNodeBuilder<>(
				BoundPojoModelPath.root( rawTypeModel ), buildingHelper
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + rawTypeModel + "]";
	}

	void closeOnFailure() {
		containingEntitiesResolverRootBuilder.closeOnFailure();
	}

	void addDirtyPathTriggeringSelfReindexing(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		checkNotFrozen();
		dirtyPathsTriggeringSelfReindexing.add( dirtyPathFromEntityType.toUnboundPath() );
	}

	void addContainingAssociationPath(PojoModelPathValueNode pathFromContainedSide,
			PojoRawTypeModel<?> containingType, PojoModelPathValueNode pathFromContainingSide) {
		checkNotFrozen();
		containingAssociationPaths.computeIfAbsent( pathFromContainedSide, ignored -> new LinkedHashMap<>() )
				.put( containingType, pathFromContainingSide );
	}

	PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T> containingEntitiesResolverRoot() {
		return containingEntitiesResolverRootBuilder;
	}

	final Optional<PojoImplicitReindexingResolver<T>> build() {
		freeze();

		PojoRuntimePathsBuildingHelper pathsBuildingHelper = buildingHelper.runtimePathsBuildingHelper( rawTypeModel );

		Set<PojoModelPathValueNode> immutableDirtyPathsAcceptedByFilter = dirtyPathsTriggeringSelfReindexing;

		Optional<PojoImplicitReindexingResolverNode<T>> containingEntitiesResolverRootOptional =
				containingEntitiesResolverRootBuilder.build( pathsBuildingHelper, null );

		if ( immutableDirtyPathsAcceptedByFilter.isEmpty() && !containingEntitiesResolverRootOptional.isPresent()
				&& containingAssociationPaths.isEmpty() ) {
			/*
			 * If this resolver won't resolve to anything, it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			PojoPathFilter dirtySelfFilter = pathsBuildingHelper.createFilter( immutableDirtyPathsAcceptedByFilter );

			PojoImplicitReindexingResolverNode<T> containingEntitiesResolverRoot =
					containingEntitiesResolverRootOptional.orElseGet( PojoImplicitReindexingResolverNode::noOp );

			Set<PojoModelPathValueNode> dirtySelfOrContainingPaths =
					new HashSet<>( immutableDirtyPathsAcceptedByFilter );
			dirtySelfOrContainingPaths.addAll(
					containingEntitiesResolverRootBuilder.getDirtyPathsTriggeringReindexingIncludingNestedNodes() );
			PojoPathFilter dirtySelfOrContainingFilter = pathsBuildingHelper.createFilter( dirtySelfOrContainingPaths );

			return Optional.of( new PojoImplicitReindexingResolverImpl<>( dirtySelfFilter, dirtySelfOrContainingFilter,
					containingEntitiesResolverRoot,
					buildingHelper.createAssociationInverseSideResolver( rawTypeModel, containingAssociationPaths ) ) );
		}
	}

	/**
	 * Freeze the builder, signaling that no mutating method will be called anymore
	 * and that derived data can be safely computed.
	 */
	private void freeze() {
		if ( !frozen ) {
			frozen = true;
			containingEntitiesResolverRootBuilder.freeze();
		}
	}

	private void checkNotFrozen() {
		if ( frozen ) {
			throw new AssertionFailure(
					"A mutating method was called on " + this + " after it was frozen."
			);
		}
	}

	static class Walker implements PojoModelPathWalker<
			Void, AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?>,
			PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?>,
			PojoImplicitReindexingResolverValueNodeBuilderDelegate<?>
			> {
		public static final Walker INSTANCE = new Walker();

		@Override
		public PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> property(
				Void context, AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> typeNode,
				PojoModelPathPropertyNode pathNode) {
			return typeNode.property( pathNode.propertyName() );
		}

		@Override
		public PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> value(
				Void context, PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> propertyNode,
				PojoModelPathValueNode pathNode) {
			return propertyNode.value( pathNode.extractorPath() );
		}

		@Override
		public AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> type(
				Void context, PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNode) {
			return valueNode.type();
		}
	}
}
