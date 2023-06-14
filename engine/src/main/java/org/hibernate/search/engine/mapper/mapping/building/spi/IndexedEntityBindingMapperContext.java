/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;

public interface IndexedEntityBindingMapperContext {

	/**
	 * Get the shared path-tracker for a given indexed-embedded definition.
	 * <p>
	 * A single definition may lead to multiple indexed-embedded "instances":
	 * in sub-types of the type defining the indexed-embedded (the "hosting type"),
	 * or simply at a deeper level when the indexed-embedded points to the same type as the hosting type
	 * (recursive indexed-embedded).
	 * In such case, each "instance" of the indexed-embedded may use different parts of the definition,
	 * so using a single path tracker gives it a complete views
	 * and allows it to decide whether an includePaths is useful or not.
	 *
	 * @param definition An indexed embedded definition.
	 * @return The path tracker for that definition.
	 */
	TreeFilterPathTracker getOrCreatePathTracker(IndexedEmbeddedDefinition definition);

}
