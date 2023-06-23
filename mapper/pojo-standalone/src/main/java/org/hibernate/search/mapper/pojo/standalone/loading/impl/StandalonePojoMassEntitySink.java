/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;

public class StandalonePojoMassEntitySink<E> implements MassEntitySink<E> {

	private final PojoMassEntitySink<E> delegate;

	public StandalonePojoMassEntitySink(PojoMassEntitySink<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void accept(List<? extends E> batch) throws InterruptedException {
		delegate.accept( batch );
	}

}
