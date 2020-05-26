/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverMultiNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverDirtinessFilterNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.util.common.AssertionFailure;

abstract class AbstractPojoImplicitReindexingResolverNodeBuilder<T> {

	final PojoImplicitReindexingResolverBuildingHelper buildingHelper;

	private boolean frozen = false;
	// Use a LinkedHashSet for deterministic iteration
	private final Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingIncludingNestedNodes = new LinkedHashSet<>();

	AbstractPojoImplicitReindexingResolverNodeBuilder(PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this.buildingHelper = buildingHelper;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getModelPath() + "]";
	}

	abstract BoundPojoModelPath getModelPath();

	abstract void closeOnFailure();

	/**
	 * Freeze the builder, signaling that no mutating method will be called anymore
	 * and that derived data can be safely computed.
	 */
	final void freeze() {
		if ( !frozen ) {
			frozen = true;
			onFreeze( dirtyPathsTriggeringReindexingIncludingNestedNodes );
		}
	}

	abstract void onFreeze(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingCollector);

	final void checkNotFrozen() {
		if ( frozen ) {
			throw new AssertionFailure(
					"A mutating method was called on " + this + " after it was frozen."
					+ " There is a bug in Hibernate Search, please report it."
			);
		}
	}

	final void checkFrozen() {
		if ( !frozen ) {
			throw new AssertionFailure(
					"A method was called on " + this + " before it was frozen, but a preliminary freeze is required."
					+ " There is a bug in Hibernate Search, please report it."
			);
		}
	}

	final Set<PojoModelPathValueNode> getDirtyPathsTriggeringReindexingIncludingNestedNodes() {
		checkFrozen();
		return dirtyPathsTriggeringReindexingIncludingNestedNodes;
	}

	/**
	 * @param pathFilterFactory A factory for path filters that will be used in the resolver (and its nested resolvers)
	 * @param allPotentialDirtyPaths A comprehensive list of all paths that may be dirty
	 * when the built resolver will be called. {@code null} if unknown.
	 * @param <S> The expected type of the objects representing a set of paths at runtime.
	 */
	final <S> Optional<PojoImplicitReindexingResolverNode<T, S>> build(PojoPathFilterFactory<S> pathFilterFactory,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		freeze();

		Set<PojoModelPathValueNode> immutableDirtyPathsAcceptedByFilter =
				getDirtyPathsTriggeringReindexingIncludingNestedNodes();

		Optional<PojoImplicitReindexingResolverNode<T, S>> result;

		/*
		 * The following code allows us to decide whether we need a path filter
		 * (a PojoImplicitReindexingResolverDirtinessFilterNode) to wrap our node,
		 * depending on the dirty paths we may encounter.
		 */
		if ( allPotentialDirtyPaths == null
				|| !immutableDirtyPathsAcceptedByFilter.containsAll( allPotentialDirtyPaths ) ) {
			/*
			 * Either:
			 * - The node we are building is the root node, thus we don't have a clue about
			 * which dirty path might be submitted to us (allPotentialDirtyPaths = null, meaning unknown).
			 * - The node we are building is the child of another node,
			 * but ancestor nodes are triggered by a broader sets of dirty paths than our node,
			 * because sibling nodes are triggered by different dirty paths:
			 * the resolveEntitiesToReindex method may
			 * still be called with a dirtiness state that only contains paths that should not trigger
			 * reindexing by this node.
			 *
			 * Thus we need to filter out all the paths that are not tied to this node.
			 */
			result = doBuild( pathFilterFactory, immutableDirtyPathsAcceptedByFilter );
			if ( result.isPresent() ) {
				result = Optional.of(
						wrapWithFilter( result.get(), pathFilterFactory, immutableDirtyPathsAcceptedByFilter )
				);
			}
		}
		else {
			/*
			 * Optimization avoiding to add redundant filters.
			 *
			 * The node we are building is the child of another node,
			 * and the ancestor nodes filter input enough so that the resolveEntitiesToReindex method will
			 * only be called with a dirtiness state that only contains paths that should trigger
			 * reindexing by this node.
			 *
			 * This will happen when the node we are building is the only child of its parent node,
			 * or when all of its sibling nodes are triggered by the same paths as the node we are building,
			 * or on a subset of those paths.
			 *
			 * Thus we do not need to add our own dirty check: no filter node wrapping the node we are building
			 * is necessary.
			 */
			result = doBuild( pathFilterFactory, allPotentialDirtyPaths );
		}

		if ( !result.isPresent() ) {
			// If for some reason this node is not used, it may still hold resources that should be closed
			closeOnFailure();
		}

		return result;
	}

	abstract <S> Optional<PojoImplicitReindexingResolverNode<T, S>> doBuild(PojoPathFilterFactory<S> pathFilterFactory,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths);

	private <S> PojoImplicitReindexingResolverNode<T, S> wrapWithFilter(PojoImplicitReindexingResolverNode<T, S> resolver,
			PojoPathFilterFactory<S> pathFilterFactory,
			Set<PojoModelPathValueNode> immutableDirtyPathsTriggeringReindexing) {
		PojoPathFilter<S> filter = pathFilterFactory.create( immutableDirtyPathsTriggeringReindexing );
		return new PojoImplicitReindexingResolverDirtinessFilterNode<>(
				filter, resolver
		);
	}

	protected final <T2, S> PojoImplicitReindexingResolverNode<? super T2, S> createNested(
			Collection<? extends PojoImplicitReindexingResolverNode<? super T2, S>> elements) {
		int size = elements.size();
		if ( size == 0 ) {
			// Simplify the tree: no need for a node here
			return PojoImplicitReindexingResolverNode.noOp();
		}
		else if ( size == 1 ) {
			// Simplify the tree: no need for a multi-node here
			return elements.iterator().next();
		}
		else {
			return new PojoImplicitReindexingResolverMultiNode<>( elements );
		}
	}
}
