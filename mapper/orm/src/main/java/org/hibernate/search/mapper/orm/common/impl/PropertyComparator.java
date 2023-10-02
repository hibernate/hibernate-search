/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.common.impl;

import java.util.Comparator;

import org.hibernate.mapping.Property;

final class PropertyComparator implements Comparator<Property> {
	public static final Comparator<? super Property> INSTANCE = new PropertyComparator();

	private PropertyComparator() {
	}

	@Override
	public int compare(Property o1, Property o2) {
		return o1.getName().compareTo( o2.getName() );
	}
}
