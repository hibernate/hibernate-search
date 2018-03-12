/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.StreamPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.spi.Closer;
import org.hibernate.search.util.spi.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class PojoMappingDelegateImpl implements PojoMappingDelegate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoTypeManagerContainer typeManagers;

	public PojoMappingDelegateImpl(PojoTypeManagerContainer typeManagers) {
		this.typeManagers = typeManagers;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoTypeManager::close, typeManagers.getAll() );
		}
	}

	@Override
	public ChangesetPojoWorker createWorker(PojoSessionContext sessionContext) {
		return new ChangesetPojoWorkerImpl( typeManagers, sessionContext );
	}

	@Override
	public StreamPojoWorker createStreamWorker(PojoSessionContext sessionContext) {
		return new StreamPojoWorkerImpl( typeManagers, sessionContext );
	}

	@Override
	public <T> PojoSearchTargetDelegate<T> createPojoSearchTarget(Collection<? extends Class<? extends T>> targetedTypes,
			SessionContext sessionContext) {
		if ( targetedTypes.isEmpty() ) {
			throw log.cannotSearchOnEmptyTarget();
		}
		Set<PojoTypeManager<?, ? extends T, ?>> targetedTypeManagers = targetedTypes.stream()
				.flatMap( t -> typeManagers.getAllBySuperClass( t )
						.orElseThrow( () -> new SearchException( "Type " + t + " is not indexed and hasn't any indexed supertype." ) )
						.stream()
				)
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		return new PojoSearchTargetDelegateImpl<>( typeManagers, targetedTypeManagers, sessionContext );
	}

	@Override
	public boolean isIndexable(Class<?> type) {
		return typeManagers.getByExactClass( type ).isPresent();
	}

	@Override
	public boolean isSearchable(Class<?> type) {
		return typeManagers.getAllBySuperClass( type ).isPresent();
	}
}
