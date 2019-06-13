/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeDocumentWorkExecutor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoSessionWorkExecutorImpl implements PojoSessionWorkExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final AbstractPojoSessionContextImplementor sessionContext;
	private final PojoRuntimeIntrospector introspector;
	private final DocumentCommitStrategy commitStrategy;

	private final Map<Class<?>, PojoTypeDocumentWorkExecutor<?, ?, ?>> typeExecutors = new HashMap<>();

	public PojoSessionWorkExecutorImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			AbstractPojoSessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy) {
		this.indexedTypeManagers = indexedTypeManagers;
		this.sessionContext = sessionContext;
		this.introspector = sessionContext.getRuntimeIntrospector();
		this.commitStrategy = commitStrategy;
	}

	@Override
	public CompletableFuture<?> add(Object entity) {
		return add( null, entity );
	}

	@Override
	public CompletableFuture<?> add(Object providedId, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		PojoTypeDocumentWorkExecutor<?, ?, ?> typeExecutor = this.typeExecutors.get( clazz );
		if ( typeExecutor == null ) {
			typeExecutor = createTypeDocumentExecutor( clazz );
			typeExecutors.put( clazz, typeExecutor );
		}

		return typeExecutor.add( providedId, entity );
	}

	private PojoTypeDocumentWorkExecutor<?, ?, ?> createTypeDocumentExecutor(Class<?> clazz) {
		Optional<? extends PojoIndexedTypeManager<?, ?, ?>> exactClass = indexedTypeManagers.getByExactClass( clazz );
		if ( !exactClass.isPresent() ) {
			throw log.notDirectlyIndexedType( clazz );
		}

		return exactClass.get().createDocumentWorkExecutor( sessionContext, commitStrategy );
	}
}
