/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.util.List;

import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.javabean.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class JavaBeanLoader<E> implements PojoLoader<E> {

	private final EntityLoader<E> delegate;

	public JavaBeanLoader(EntityLoader<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<?> loadBlocking(List<?> identifiers, Deadline deadline) {
		return delegate.load( identifiers );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E2 extends E> E2 castToExactTypeOrNull(PojoRawTypeIdentifier<E2> expectedType, Object loadedObject) {
		if ( expectedType.javaClass().equals( loadedObject.getClass() ) ) {
			return (E2) loadedObject;
		}
		else {
			return null;
		}
	}
}
