/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * @param <P> The type of the property.
 */
class PojoPropertyElementAccessor<P> implements PojoElementAccessor<P> {

	private final PojoElementAccessor<?> parent;
	private final ValueReadHandle<P> handle;
	private final PojoModelPathValueNode path;

	PojoPropertyElementAccessor(PojoElementAccessor<?> parent, ValueReadHandle<P> handle,
			PojoModelPathValueNode path) {
		this.parent = parent;
		this.handle = handle;
		this.path = path;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "path=" + path
				+ "]";
	}

	@Override
	public P read(Object parentElement) {
		Object parentValue = parent.read( parentElement );
		if ( parentValue != null ) {
			return handle.get( parentValue );
		}
		else {
			return null;
		}
	}

}
