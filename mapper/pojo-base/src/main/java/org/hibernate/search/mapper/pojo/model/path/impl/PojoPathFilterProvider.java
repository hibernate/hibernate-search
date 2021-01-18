/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.BitSet;
import java.util.Set;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathsDefinition;
import org.hibernate.search.util.common.impl.CollectionHelper;

/**
 * A provider of {@link PojoPathFilter} instances.
 */
public final class PojoPathFilterProvider {

	private final PojoPathsDefinition definitions;
	private final PojoPathOrdinals ordinals = new PojoPathOrdinals();

	public PojoPathFilterProvider(PojoPathsDefinition definitions) {
		this.definitions = definitions;
		for ( String path : definitions.preDefinedOrdinals() ) {
			ordinals.toExistingOrNewOrdinal( path );
		}
	}

	/**
	 * @param paths The set of paths to test for dirtiness.
	 * The set must be non-null and non-empty, and the elements must be non-null.
	 * Container value extractor paths must be completely resolved:
	 * {@link ContainerExtractorPath#defaultExtractors()} is an invalid value
	 * that must never appear in the given paths.
	 * @return A filter accepting only the given paths.
	 */
	public PojoPathFilter create(Set<PojoModelPathValueNode> paths) {
		// Use a LinkedHashSet for deterministic iteration
		Set<String> pathsAsStrings = CollectionHelper.newLinkedHashSet( paths.size() );
		definitions.interpretPaths( pathsAsStrings, paths );

		BitSet acceptedPaths = new BitSet();
		for ( String pathsAsString : pathsAsStrings ) {
			acceptedPaths.set( ordinals.toExistingOrNewOrdinal( pathsAsString ) );
		}
		return new PojoPathFilter( ordinals, acceptedPaths );
	}

}
