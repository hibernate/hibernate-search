/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public final class IndexedEmbeddedDefinition {

	private final MappableTypeModel definingTypeModel;
	private final String relativePrefix;
	private final ObjectFieldStorage storage;
	private final Set<String> includePaths;
	private final Integer maxDepth;

	/**
	 * @param definingTypeModel The model representing the type on which the indexed-embedded was defined.
	 * @param relativePrefix The prefix to apply to all index fields created in the context of the indexed-embedded.
	 * @param storage The storage type to use for all object fields created as part of the {@code relativePrefix}.
	 * @param maxDepth The maximum depth beyond which all created fields will be ignored. {@code null} for no limit.
	 * @param includePaths The exhaustive list of paths of fields that are to be included. {@code null} for no limit.
	 */
	public IndexedEmbeddedDefinition(MappableTypeModel definingTypeModel, String relativePrefix,
			ObjectFieldStorage storage, Integer maxDepth,
			Set<String> includePaths) {
		this.definingTypeModel = definingTypeModel;
		this.relativePrefix = relativePrefix;
		this.storage = storage;
		this.includePaths = includePaths == null ? Collections.emptySet() : new LinkedHashSet<>( includePaths );
		if ( maxDepth == null && !this.includePaths.isEmpty() ) {
			/*
			 * If no max depth was provided and included paths were provided,
			 * the remaining composition depth is implicitly set to 0,
			 * meaning no composition is allowed and paths are excluded unless
			 * explicitly listed in "includePaths".
			 */
			this.maxDepth = 0;
		}
		else {
			this.maxDepth = maxDepth;
		}
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
				Objects.equals( maxDepth, that.maxDepth ) &&
				includePaths.equals( that.includePaths );
	}

	@Override
	public int hashCode() {
		return Objects.hash( definingTypeModel, relativePrefix, maxDepth, includePaths );
	}

	public MappableTypeModel getDefiningTypeModel() {
		return definingTypeModel;
	}

	public String getRelativePrefix() {
		return relativePrefix;
	}

	public ObjectFieldStorage getStorage() {
		return storage;
	}

	public Set<String> getIncludePaths() {
		return includePaths;
	}

	public Integer getMaxDepth() {
		return maxDepth;
	}

}
