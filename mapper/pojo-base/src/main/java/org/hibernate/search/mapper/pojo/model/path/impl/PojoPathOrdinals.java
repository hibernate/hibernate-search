/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class PojoPathOrdinals {

	private final Map<String, Integer> ordinalByPath = new HashMap<>();
	private final List<String> pathByOrdinal = new ArrayList<>();

	@Override
	public String toString() {
		return "PojoPathOrdinals{"
				+ "ordinalByPath=" + ordinalByPath
				+ '}';
	}

	public Integer toOrdinal(String path) {
		return ordinalByPath.get( path );
	}

	public String toPath(int ordinal) {
		return ordinal < pathByOrdinal.size() ? pathByOrdinal.get( ordinal ) : null;
	}

	public BitSet toPathSelection(Collection<String> paths) {
		if ( paths.isEmpty() ) {
			return null;
		}
		BitSet bitSet = null;
		for ( String path : paths ) {
			Integer ordinal = toOrdinal( path );
			if ( ordinal == null ) {
				continue;
			}
			if ( bitSet == null ) {
				bitSet = new BitSet();
			}
			bitSet.set( ordinal );
		}
		return bitSet;
	}

	public Set<String> toPathSet(BitSet pathSelection) {
		if ( pathSelection == null ) {
			return Collections.emptySet();
		}
		return pathSelection.stream().mapToObj( this::toPath )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	public int toExistingOrNewOrdinal(String path) {
		Integer ordinal = ordinalByPath.get( path );
		if ( ordinal != null ) {
			return ordinal;
		}
		pathByOrdinal.add( path );
		ordinal = pathByOrdinal.size() - 1;
		ordinalByPath.put( path, ordinal );
		return ordinal;
	}
}
