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

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverMarkingNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoRuntimePathsBuildingHelper;

class PojoImplicitReindexingResolverMarkingNodeBuilder<T>
		extends AbstractPojoImplicitReindexingResolverNodeBuilder<T> {

	private final BoundPojoModelPath modelPath;

	// Use a LinkedHashSet for deterministic iteration
	private final Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexing = new LinkedHashSet<>();

	PojoImplicitReindexingResolverMarkingNodeBuilder(BoundPojoModelPathTypeNode<T> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPath = modelPath;
	}

	@Override
	BoundPojoModelPath getModelPath() {
		return modelPath;
	}

	@Override
	void closeOnFailure() {
		// Nothing to do
	}

	void addDirtyPathTriggeringReindexing(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		checkNotFrozen();
		dirtyPathsTriggeringReindexing.add( dirtyPathFromEntityType.toUnboundPath() );
	}

	@Override
	void onFreeze(Set<PojoModelPathValueNode> dirtyPathsTriggeringReindexingCollector) {
		dirtyPathsTriggeringReindexingCollector.addAll( dirtyPathsTriggeringReindexing );
	}

	@Override
	Optional<PojoImplicitReindexingResolverNode<T>> doBuild(PojoRuntimePathsBuildingHelper pathsBuildingHelper,
			Set<PojoModelPathValueNode> allPotentialDirtyPaths) {
		checkFrozen();

		boolean markForReindexing = !dirtyPathsTriggeringReindexing.isEmpty();

		if ( !markForReindexing ) {
			/*
			 * If this resolver doesn't mark anything as "to reindex",
			 * then it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoImplicitReindexingResolverMarkingNode<>() );
		}
	}

}
