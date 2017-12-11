/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.BridgedElement;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElementReader;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

class PojoPropertyBridgedElementReader<T> implements BridgedElementReader<T> {

	private final BridgedElementReader<?> parent;
	private final PropertyHandle handle;

	PojoPropertyBridgedElementReader(BridgedElementReader<?> parent, PropertyHandle handle) {
		this.parent = parent;
		this.handle = handle;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getType() {
		return (Class<T>) handle.getType();
	}

	@Override
	public T read(BridgedElement bridgedElement) {
		return (T) handle.get( parent.read( bridgedElement ) );
	}

}