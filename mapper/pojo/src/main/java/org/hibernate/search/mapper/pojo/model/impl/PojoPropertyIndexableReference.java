/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

class PojoPropertyIndexableReference<T> implements PojoIndexableReference<T> {

	private final PojoIndexableReference<?> parent;
	private final PropertyHandle handle;

	PojoPropertyIndexableReference(PojoIndexableReference<?> parent, PropertyHandle handle) {
		this.parent = parent;
		this.handle = handle;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getType() {
		return (Class<T>) handle.getType();
	}

	@Override
	public T get(Object root) {
		return (T) handle.get( parent.get( root ) );
	}

}