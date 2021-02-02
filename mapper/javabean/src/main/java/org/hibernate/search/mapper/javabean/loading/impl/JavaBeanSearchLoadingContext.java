/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class JavaBeanSearchLoadingContext implements PojoLoadingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private JavaBeanSearchLoadingContext() {
	}

	@Override
	public void checkOpen() {
		// Nothing to do: we're always "open",
		// but don't ever try to use createLoader(), as *that* will fail.
	}

	@Override
	public Object loaderKey(PojoRawTypeIdentifier<?> type) {
		return type;
	}

	@Override
	public <T> PojoLoader<T> createLoader(Set<PojoRawTypeIdentifier<? extends T>> expectedTypes) {
		throw log.entityLoadingNotSupported();
	}

	public static final class Builder implements PojoLoadingContextBuilder<Void> {
		public Builder() {
		}

		@Override
		public Void toAPI() {
			throw log.entityLoadingNotSupported();
		}

		@Override
		public PojoLoadingContext build() {
			return new JavaBeanSearchLoadingContext();
		}
	}
}
