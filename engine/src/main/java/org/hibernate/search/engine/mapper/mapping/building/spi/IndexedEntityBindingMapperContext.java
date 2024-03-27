/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;

public interface IndexedEntityBindingMapperContext {

	/**
	 * Get the shared path-tracker for a given mapping element involving a {@link TreeFilterDefinition},
	 * e.g. an {@code @IndexedEmbedded}.
	 * <p>
	 * A single mapping element may lead to multiple "uses":
	 * for example an indexed-embedded may be inherited by sub-types of the type defining the indexed-embedded (the "hosting type"),
	 * or an indexed-embedded may recurse (directly or indirectly) into the the type defining the indexed-embedded.
	 * In such case, each "use" of the indexed-embedded may occur in a different context
	 * and thus use different parts of the definition,
	 * so we need a single, shared path tracker across all occurrences
	 * to gives us a complete view of encountered paths and to allow us to decide
	 * whether an includePaths/excludePaths is useful or not.
	 *
	 * @param mappingElement A unique representation of the mapping element involving a tree filter;
	 * if the same mapping element is applied in multiple places,
	 * this method must be called with mapping elements that are equal according to {@link MappingElement#equals(Object)}/{@link MappingElement#hashCode()}.
	 * @param filterDefinition The filter definition passed to the {@link TreeFilterPathTracker} upon creation.
	 * @return The path tracker for that definition.
	 */
	TreeFilterPathTracker getOrCreatePathTracker(MappingElement mappingElement, TreeFilterDefinition filterDefinition);

}
