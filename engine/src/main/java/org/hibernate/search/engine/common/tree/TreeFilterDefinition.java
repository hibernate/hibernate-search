/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.tree;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class TreeFilterDefinition {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final TreeFilterDefinition INCLUDE_ALL = new TreeFilterDefinition( null, null, null );

	public static TreeFilterDefinition includeAll() {
		return INCLUDE_ALL;
	}

	private final Set<String> includePaths;
	private final Set<String> excludePaths;
	private final Integer includeDepth;

	/**
	 * @param includeDepth The maximum depth beyond which all created fields will be ignored. {@code null} for no limit.
	 * @param includePaths The exhaustive list of paths of fields that are to be included. {@code null} for no limit.
	 * Cannot be used with a non-empty {@code excludePaths}.
	 * @param excludePaths The list of paths of fields that are to be excluded. {@code null} for no exclusion.
	 * Cannot be used with a non-empty {@code includePaths}.
	 */
	public TreeFilterDefinition(Integer includeDepth, Set<String> includePaths, Set<String> excludePaths) {
		this.includePaths = includePaths == null ? Collections.emptySet() : new LinkedHashSet<>( includePaths );
		this.excludePaths = excludePaths == null ? Collections.emptySet() : new LinkedHashSet<>( excludePaths );
		if ( !this.includePaths.isEmpty() && !this.excludePaths.isEmpty() ) {
			throw log.cannotIncludeAndExcludePathsWithinSameFilter(
					includePaths,
					excludePaths
			);
		}
		if ( includeDepth == null && !this.includePaths.isEmpty() ) {
			/*
			 * If no max depth was provided and included paths were provided,
			 * the remaining composition depth is implicitly set to 0,
			 * meaning no composition is allowed and paths are excluded unless
			 * explicitly listed in "includePaths".
			 */
			this.includeDepth = 0;
		}
		else {
			this.includeDepth = includeDepth;
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
		TreeFilterDefinition that = (TreeFilterDefinition) o;
		return Objects.equals( includeDepth, that.includeDepth )
				&& includePaths.equals( that.includePaths )
				&& excludePaths.equals( that.excludePaths );
	}

	@Override
	public int hashCode() {
		return Objects.hash( includeDepth, includePaths, excludePaths );
	}

	public Set<String> includePaths() {
		return includePaths;
	}

	public Set<String> excludePaths() {
		return excludePaths;
	}

	public Integer includeDepth() {
		return includeDepth;
	}

}
