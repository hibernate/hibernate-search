/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class PojoSessionWorkExecutorImpl implements PojoSessionWorkExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoContainedTypeManagerContainer containedTypeManagers;
	private final AbstractPojoSessionContextImplementor sessionContext;
	private final PojoRuntimeIntrospector introspector;

	private Map<Class<?>, IndexWorkExecutor> indexWorkExecutor;

	public PojoSessionWorkExecutorImpl(PojoIndexedTypeManagerContainer indexedTypeManagers, PojoContainedTypeManagerContainer containedTypeManagers,
			AbstractPojoSessionContextImplementor sessionContext) {
		this.indexedTypeManagers = indexedTypeManagers;
		this.containedTypeManagers = containedTypeManagers;
		this.sessionContext = sessionContext;
		this.introspector = sessionContext.getRuntimeIntrospector();
	}

	@Override
	public CompletableFuture<?> add(Object id, Object entity) {
		// TODO must be implemented
		return CompletableFuture.completedFuture( "Stubbed! << must be implemented >>" );
	}

	@Override
	public CompletableFuture<?> add(Object entity) {
		return add( null, entity );
	}
}
