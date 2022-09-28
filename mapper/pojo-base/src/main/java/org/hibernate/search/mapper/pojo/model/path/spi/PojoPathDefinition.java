/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import java.util.Optional;
import java.util.Set;

/**
 * A static definition of POJO paths for a given entity type, allowing the creation of path filters.
 */
public final class PojoPathDefinition {

	private final Set<String> stringRepresentations;
	private final Optional<PojoPathEntityStateRepresentation> stateRepresentation;

	public PojoPathDefinition(Set<String> stringRepresentations,
			Optional<PojoPathEntityStateRepresentation> entityStateRepresentation) {
		this.stringRepresentations = stringRepresentations;
		this.stateRepresentation = entityStateRepresentation;
	}

	/**
	 * @return The string representations of this path.
	 */
	public Set<String> stringRepresentations() {
		return stringRepresentations;
	}

	public Optional<PojoPathEntityStateRepresentation> entityStateRepresentation() {
		return stateRepresentation;
	}

}
