/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.DefaultPojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathWalker;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.AssertionFailure;

class PojoImplicitReindexingResolverBuilder<T> {

	static Walker walker() {
		return Walker.INSTANCE;
	}

	private final PojoRawTypeModel<T> rawTypeModel;

	// Use a LinkedHashSet for deterministic iteration
	private final Set<PojoModelPathValueNode> dirtyPathsTriggeringSelfReindexing = new LinkedHashSet<>();

	private final PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T> containingEntitiesResolverRootBuilder;

	private boolean frozen = false;

	PojoImplicitReindexingResolverBuilder(PojoRawTypeModel<T> rawTypeModel,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this.rawTypeModel = rawTypeModel;
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

	PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T> containingEntitiesResolverRoot() {
		return containingEntitiesResolverRootBuilder;
	}

	/**
	 * @param pathFilterFactory A factory for path filters that will be used in the resolver (and its nested resolvers)
	 * @param <S> The expected type of the objects representing a set of paths at runtime.
	 */
	final <S> Optional<PojoImplicitReindexingResolver<T, S>> build(PojoPathFilterFactory<S> pathFilterFactory) {
		freeze();

		Set<PojoModelPathValueNode> immutableDirtyPathsAcceptedByFilter = dirtyPathsTriggeringSelfReindexing;

		Optional<PojoImplicitReindexingResolverNode<T, S>> containingEntitiesResolverRootOptional =
				containingEntitiesResolverRootBuilder.build( pathFilterFactory, null );

		if ( immutableDirtyPathsAcceptedByFilter.isEmpty() && !containingEntitiesResolverRootOptional.isPresent() ) {
			/*
			 * If this resolver won't resolve to anything, it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			PojoPathFilter<S> filter = immutableDirtyPathsAcceptedByFilter.isEmpty()
					? PojoPathFilter.empty() : pathFilterFactory.create( immutableDirtyPathsAcceptedByFilter );
			PojoImplicitReindexingResolverNode<T, S> containingEntitiesResolverRoot =
					containingEntitiesResolverRootOptional.orElseGet( PojoImplicitReindexingResolverNode::noOp );

			return Optional.of(
					new DefaultPojoImplicitReindexingResolver<>( filter, containingEntitiesResolverRoot )
			);
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
							+ " There is a bug in Hibernate Search, please report it."
			);
		}
	}

	static class Walker implements PojoModelPathWalker<
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?>,
			PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?>,
			PojoImplicitReindexingResolverValueNodeBuilderDelegate<?>
			> {
		public static final Walker INSTANCE = new Walker();

		@Override
		public PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> property(
				AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> typeNode, String propertyName) {
			return typeNode.property( propertyName );
		}

		@Override
		public PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> value(
				PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> propertyNode,
				ContainerExtractorPath extractorPath) {
			return propertyNode.value( extractorPath );
		}

		@Override
		public AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> type(
				PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNode) {
			return valueNode.type();
		}
	}
}
