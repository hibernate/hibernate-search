/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.StreamPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class PojoMappingDelegateImpl implements PojoMappingDelegate {

	private final PojoTypeManagerContainer typeManagers;

	public PojoMappingDelegateImpl(PojoTypeManagerContainer typeManagers) {
		this.typeManagers = typeManagers;
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
	public PojoSearchTarget createPojoSearchTarget(Collection<? extends Class<?>> targetedTypes) {
		Set<PojoTypeManager<?, ?, ?>> targetedTypeManagers;
		if ( targetedTypes.isEmpty() ) {
			targetedTypeManagers = typeManagers.getAll();
		}
		else {
			targetedTypeManagers = targetedTypes.stream()
					.flatMap( t -> typeManagers.getAllBySuperType( t )
							.orElseThrow( () -> new SearchException( "Type " + t + " is not indexed and hasn't any indexed supertype." ) )
							.stream()
					)
					.collect( Collectors.toCollection( LinkedHashSet::new ) );
		}
		return new PojoSearchTargetImpl( typeManagers, targetedTypeManagers );
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
