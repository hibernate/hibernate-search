/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common.impl;

import java.util.Comparator;

import org.hibernate.mapping.Property;

public class PropertyComparator implements Comparator<Property> {
	public static final Comparator<? super Property> INSTANCE = new PropertyComparator();

	private PropertyComparator() {
	}

	@Override
	public int compare(Property o1, Property o2) {
		return o1.getName().compareTo( o2.getName() );
	}
}
