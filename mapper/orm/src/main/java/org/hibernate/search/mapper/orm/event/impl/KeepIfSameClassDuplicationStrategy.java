/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import org.hibernate.event.service.spi.DuplicationStrategy;

class KeepIfSameClassDuplicationStrategy implements DuplicationStrategy {
	private final Class<?> checkClass;

	public KeepIfSameClassDuplicationStrategy(Class<?> checkClass) {
		this.checkClass = checkClass;
	}

	@Override
	public boolean areMatch(Object listener, Object original) {
		// not isAssignableFrom since the user could subclass
		return checkClass == original.getClass() && checkClass == listener.getClass();
	}

	@Override
	public Action getAction() {
		return Action.KEEP_ORIGINAL;
	}
}
