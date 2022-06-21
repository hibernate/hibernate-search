/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;

public class StandalonePojoMassIdentifierLoader implements PojoMassIdentifierLoader {

	private final MassIdentifierLoader delegate;

	public StandalonePojoMassIdentifierLoader(MassIdentifierLoader delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public long totalCount() {
		return delegate.totalCount();
	}

	@Override
	public void loadNext() throws InterruptedException {
		delegate.loadNext();
	}
}
