/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Objects;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public final class IndexedEmbeddedDefinition {

	private final MappableTypeModel definingTypeModel;
	private final String relativePrefix;
	private final ObjectStructure structure;
	private final TreeFilterDefinition filter;

	/**
	 * @param definingTypeModel The model representing the type on which the indexed-embedded was defined.
	 * @param relativePrefix The prefix to apply to all index fields created in the context of the indexed-embedded.
	 * @param structure The structure of all object fields created as part of the {@code relativePrefix}.
	 * @param filter The filter definition (included paths, ...).
	 */
	public IndexedEmbeddedDefinition(MappableTypeModel definingTypeModel, String relativePrefix,
			ObjectStructure structure, TreeFilterDefinition filter) {
		this.definingTypeModel = definingTypeModel;
		this.relativePrefix = relativePrefix;
		this.structure = structure;
		this.filter = filter;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		IndexedEmbeddedDefinition that = (IndexedEmbeddedDefinition) o;
		return definingTypeModel.equals( that.definingTypeModel ) &&
				relativePrefix.equals( that.relativePrefix ) &&
				filter.equals( that.filter );
	}

	@Override
	public int hashCode() {
		return Objects.hash( definingTypeModel, relativePrefix, filter );
	}

	public MappableTypeModel definingTypeModel() {
		return definingTypeModel;
	}

	public String relativePrefix() {
		return relativePrefix;
	}

	public ObjectStructure structure() {
		return structure;
	}

	public TreeFilterDefinition filter() {
		return filter;
	}

}
