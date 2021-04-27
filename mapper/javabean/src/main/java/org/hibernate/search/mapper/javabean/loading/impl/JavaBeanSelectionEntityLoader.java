/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.util.List;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.mapper.javabean.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;

public class JavaBeanSelectionEntityLoader<E> implements PojoSelectionEntityLoader<E> {

	private final SelectionEntityLoader<E> delegate;

	public JavaBeanSelectionEntityLoader(SelectionEntityLoader<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<?> loadBlocking(List<?> identifiers, Deadline deadline) {
		return delegate.load( identifiers );
	}

}
