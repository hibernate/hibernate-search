/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

/**
 * @param <P> The type of the property.
 */
class PojoModelPropertyElementAccessor<P> implements PojoModelElementAccessor<P> {

	private final PojoModelElementAccessor<?> parent;
	private final PropertyHandle<P> handle;

	PojoModelPropertyElementAccessor(PojoModelElementAccessor<?> parent, PropertyHandle<P> handle) {
		this.parent = parent;
		this.handle = handle;
	}

	@Override
	public P read(PojoElement bridgedElement) {
		Object parentValue = parent.read( bridgedElement );
		if ( parentValue != null ) {
			return handle.get( parentValue );
		}
		else {
			return null;
		}
	}

}