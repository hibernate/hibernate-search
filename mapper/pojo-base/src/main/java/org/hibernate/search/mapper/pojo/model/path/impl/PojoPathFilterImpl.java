/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.BitSet;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;

final class PojoPathFilterImpl implements PojoPathFilter {

	private final PojoPathOrdinals ordinals;
	private final BitSet acceptedPaths;

	public PojoPathFilterImpl(PojoPathOrdinals ordinals, BitSet acceptedPaths) {
		this.ordinals = ordinals;
		this.acceptedPaths = acceptedPaths;
	}

	@Override
	public String toString() {
		return ordinals.toPathSet( acceptedPaths ).toString();
	}

	@Override
	public boolean test(BitSet pathSelection) {
		return acceptedPaths.intersects( pathSelection );
	}

	@Override
	public BitSet filter(String path) {
		Integer ordinal = ordinals.toOrdinal( path );
		if ( ordinal != null && acceptedPaths.get( ordinal ) ) {
			BitSet result = new BitSet( ordinal );
			result.set( ordinal );
			return result;
		}
		else {
			return null;
		}
	}

	@Override
	public BitSet filter(String... paths) {
		BitSet bitSet = null;
		for ( String path : paths ) {
			Integer ordinal = ordinals.toOrdinal( path );
			if ( ordinal == null || !acceptedPaths.get( ordinal ) ) {
				continue;
			}
			if ( bitSet == null ) {
				bitSet = new BitSet();
			}
			bitSet.set( ordinal );
		}
		return bitSet;
	}

	@Override
	public BitSet filter(int[] pathOrdinals) {
		BitSet bitSet = null;
		for ( int pathOrdinal : pathOrdinals ) {
			if ( !acceptedPaths.get( pathOrdinal ) ) {
				continue;
			}
			if ( bitSet == null ) {
				bitSet = new BitSet();
			}
			bitSet.set( pathOrdinal );
		}
		return bitSet;
	}

	@Override
	public BitSet filter(int pathOrdinal) {
		if ( !acceptedPaths.get( pathOrdinal ) ) {
			return null;
		}
		BitSet bitSet = new BitSet();
		bitSet.set( pathOrdinal );
		return bitSet;
	}

	@Override
	public BitSet all() {
		if ( acceptedPaths.isEmpty() ) {
			return null;
		}
		// BitSet is mutable, so we must clone it...
		return (BitSet) acceptedPaths.clone();
	}
}
