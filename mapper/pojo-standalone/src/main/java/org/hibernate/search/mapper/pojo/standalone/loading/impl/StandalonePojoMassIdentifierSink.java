/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;

public class StandalonePojoMassIdentifierSink<I> implements MassIdentifierSink<I> {

	private final PojoMassIdentifierSink<I> delegate;

	public StandalonePojoMassIdentifierSink(PojoMassIdentifierSink<I> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void accept(List<? extends I> batch) throws InterruptedException {
		delegate.accept( batch );
	}

	@Override
	public void complete() {
		delegate.complete();
	}

}
