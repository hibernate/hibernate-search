/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the structure of a tested field: located at the root or in a nested document,
 * single-valued or multi-valued, ...
 * <p>
 * Used as a test parameter in tests where the structure of fields will make Hibernate Search take a different code path,
 * such as indexing, sorts, ...
 * <p>
 * <strong>WARNING</strong>: The presence of this class as a test parameter does not auto-magically
 * make your test class test all combinations.
 * You need set up your test to create one field per structure,
 * and then query the "fieldStructure" object in your test instance to target a different field
 * based on that.
 * See for example {@code org.hibernate.search.integrationtest.backend.tck.search.sort.FieldSortBaseIT}.
 */
public class TestedFieldStructure {

	public static List<TestedFieldStructure> all() {
		return ALL;
	}

	private static final List<TestedFieldStructure> ALL;
	static {
		List<TestedFieldStructure> values = new ArrayList<>();
		for ( IndexFieldLocation location : IndexFieldLocation.values() ) {
			for ( IndexFieldValueCardinality cardinality : IndexFieldValueCardinality.values() ) {
				values.add( new TestedFieldStructure( location, cardinality ) );
			}
		}
		ALL = Collections.unmodifiableList( values );
	}

	public final IndexFieldLocation location;

	private final IndexFieldValueCardinality cardinality;

	private TestedFieldStructure(IndexFieldLocation location, IndexFieldValueCardinality cardinality) {
		this.location = location;
		this.cardinality = cardinality;
	}

	@Override
	public String toString() {
		return location + " - " + cardinality;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		TestedFieldStructure that = (TestedFieldStructure) o;
		return location == that.location && cardinality == that.cardinality;
	}

	@Override
	public int hashCode() {
		return Objects.hash( location, cardinality );
	}

	public String getUniqueName() {
		return location.name() + "_" + cardinality.name();
	}

	public boolean isInNested() {
		return location == IndexFieldLocation.IN_NESTED
				|| location == IndexFieldLocation.IN_NESTED_TWICE;
	}

	public boolean isSingleValued() {
		return IndexFieldValueCardinality.SINGLE_VALUED.equals( cardinality );
	}

	public boolean isMultiValued() {
		return !isSingleValued();
	}
}
