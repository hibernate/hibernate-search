/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

class PojoRootIndexableReference<T> implements PojoIndexableReference<T> {

	private final Class<T> type;

	PojoRootIndexableReference(Class<T> type) {
		super();
		this.type = type;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public T get(Object root) {
		return type.cast( root );
	}

}