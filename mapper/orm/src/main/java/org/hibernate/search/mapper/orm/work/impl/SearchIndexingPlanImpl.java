/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public final class SearchIndexingPlanImpl implements SearchIndexingPlan {

	private final SearchIndexingPlanTypeContextProvider typeContextProvider;
	private final SearchIndexingPlanSessionContext sessionContext;

	public SearchIndexingPlanImpl(SearchIndexingPlanTypeContextProvider typeContextProvider,
			SearchIndexingPlanSessionContext sessionContext) {
		this.typeContextProvider = typeContextProvider;
		this.sessionContext = sessionContext;
	}

	@Override
	public void addOrUpdate(Object entity) {
		delegate( true )
				.addOrUpdate( getTypeIdentifier( entity ), null, null, entity,
						true, true, null );
	}

	@Override
	public void delete(Object entity) {
		delegate( true )
				.delete( getTypeIdentifier( entity ), null, null, entity );
	}

	@Override
	public void purge(Class<?> entityClass, Object providedId, String providedRoutingKey) {
		delegate( true )
				.delete( getTypeIdentifier( entityClass ), providedId,
						DocumentRoutesDescriptor.fromLegacyRoutingKey( providedRoutingKey ), null );
	}

	@Override
	public void purge(String entityName, Object providedId, String providedRoutingKey) {
		delegate( true )
				.delete( getTypeIdentifier( entityName ), providedId,
						DocumentRoutesDescriptor.fromLegacyRoutingKey( providedRoutingKey ), null );
	}

	@Override
	public void process() {
		PojoIndexingPlan plan = delegate( false );
		if ( plan == null ) {
			return;
		}
		plan.process();
	}

	@Override
	public void execute() {
		PojoIndexingPlan plan = delegate( false );
		if ( plan == null ) {
			return;
		}
		sessionContext.configuredAutomaticIndexingSynchronizationStrategy()
				.executeAndSynchronize( plan );
	}

	private PojoIndexingPlan delegate(boolean createIfDoesNotExist) {
		sessionContext.checkOpen();
		return sessionContext.currentIndexingPlan( createIfDoesNotExist );
	}

	private <T> PojoRawTypeIdentifier<? extends T> getTypeIdentifier(T entity) {
		return sessionContext.runtimeIntrospector().detectEntityType( entity );
	}

	private PojoRawTypeIdentifier<?> getTypeIdentifier(Class<?> entityType) {
		return typeContextProvider.forExactClass( entityType ).typeIdentifier();
	}

	private PojoRawTypeIdentifier<?> getTypeIdentifier(String entityName) {
		return typeContextProvider.byEntityName().getOrFail( entityName ).typeIdentifier();
	}
}
