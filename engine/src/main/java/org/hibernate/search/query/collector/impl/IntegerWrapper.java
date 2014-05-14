/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.collector.impl;

/**
 * Replacement of Integer which is mutable, so that we can avoid creating many objects while counting hits for each facet.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public final class IntegerWrapper {
	int count = 0;

	public int getCount() {
		return count;
	}

	public void incrementCount() {
		this.count++;
	}
}


