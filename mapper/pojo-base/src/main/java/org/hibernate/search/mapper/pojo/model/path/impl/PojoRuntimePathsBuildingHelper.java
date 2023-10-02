/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.util.common.impl.CollectionHelper;

/**
 * A helper dealing with runtime paths, e.g. {@link PojoPathFilterImpl}, path ordinals, ...
 */
public final class PojoRuntimePathsBuildingHelper {

	private final PojoPathOrdinals ordinals = new PojoPathOrdinals();
	private final PojoPathDefinitionProvider pathDefinitionProvider;
	private final Map<PojoModelPathValueNode, PojoPathDefinition> pathDefinitionCache = new LinkedHashMap<>();

	public PojoRuntimePathsBuildingHelper(PojoPathDefinitionProvider pathDefinitionProvider) {
		this.pathDefinitionProvider = pathDefinitionProvider;
		for ( String path : pathDefinitionProvider.preDefinedOrdinals() ) {
			ordinals.toExistingOrNewOrdinal( path );
		}
	}

	public PojoPathDefinition toPathDefinition(PojoModelPathValueNode path) {
		return pathDefinitionCache.computeIfAbsent( path, pathDefinitionProvider::interpretPath );
	}

	public PojoPathOrdinals pathOrdinals() {
		return ordinals;
	}

	/**
	 * @param paths The set of paths to test for dirtiness.
	 * The set must be non-null and non-empty, and the elements must be non-null.
	 * Container value extractor paths must be completely resolved:
	 * {@link ContainerExtractorPath#defaultExtractors()} is an invalid value
	 * that must never appear in the given paths.
	 * @return A filter accepting only the given paths.
	 */
	public PojoPathFilter createFilter(Set<PojoModelPathValueNode> paths) {
		// Use a LinkedHashSet for deterministic iteration
		Set<String> pathsAsStrings = CollectionHelper.newLinkedHashSet( paths.size() );
		for ( PojoModelPathValueNode path : paths ) {
			pathsAsStrings.addAll( toPathDefinition( path ).stringRepresentations() );
		}

		BitSet acceptedPaths = new BitSet();
		for ( String pathsAsString : pathsAsStrings ) {
			acceptedPaths.set( ordinals.toExistingOrNewOrdinal( pathsAsString ) );
		}
		return new PojoPathFilterImpl( ordinals, acceptedPaths );
	}

	public PojoPathFilter createFilterForNonNullOrdinals(List<?> list) {
		BitSet acceptedPaths = new BitSet();
		for ( int i = 0; i < list.size(); i++ ) {
			if ( list.get( i ) != null ) {
				// We can resolve the inverse side of that association, so we need to track it.
				acceptedPaths.set( i );
			}
		}
		return new PojoPathFilterImpl( ordinals, acceptedPaths );
	}
}
