/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.util.List;

import org.hibernate.search.mapper.javabean.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;

public class JavaBeanMassEntityLoader<I> implements PojoMassEntityLoader<I> {

	private final MassEntityLoader<I> delegate;

	public JavaBeanMassEntityLoader(MassEntityLoader<I> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public void load(List<I> identifiers) {
		delegate.load( identifiers );
	}

}
