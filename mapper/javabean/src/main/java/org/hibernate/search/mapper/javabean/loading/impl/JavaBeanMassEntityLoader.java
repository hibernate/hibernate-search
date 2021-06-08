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
import org.hibernate.search.util.common.impl.Closer;

public class JavaBeanMassEntityLoader<I> implements PojoMassEntityLoader<I> {

	private final JavaBeanLoadingSessionContext session;
	private final MassEntityLoader<I> delegate;

	public JavaBeanMassEntityLoader(JavaBeanLoadingSessionContext session, MassEntityLoader<I> delegate) {
		this.session = session;
		this.delegate = delegate;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( MassEntityLoader::close, delegate );
			closer.push( JavaBeanLoadingSessionContext::close, session );
		}
	}

	@Override
	public void load(List<I> identifiers) throws InterruptedException {
		delegate.load( identifiers );
	}

}
