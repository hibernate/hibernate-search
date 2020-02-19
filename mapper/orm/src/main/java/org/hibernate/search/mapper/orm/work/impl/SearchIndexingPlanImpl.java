/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
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
		sessionContext.getCurrentIndexingPlan( true )
				.addOrUpdate( getTypeIdentifier( entity ), null, entity );
	}

	@Override
	public void delete(Object entity) {
		sessionContext.getCurrentIndexingPlan( true )
				.delete( getTypeIdentifier( entity ), null, entity );
	}

	@Override
	public void purge(Class<?> entityClass, Object providedId, String providedRoutingKey) {
		sessionContext.getCurrentIndexingPlan( true )
				.purge( getTypeIdentifier( entityClass ), providedId, providedRoutingKey );
	}

	@Override
	public void purge(String entityName, Object providedId, String providedRoutingKey) {
		sessionContext.getCurrentIndexingPlan( true )
				.purge( getTypeIdentifier( entityName ), providedId, providedRoutingKey );
	}

	@Override
	public void process() {
		PojoIndexingPlan plan = sessionContext.getCurrentIndexingPlan( false );
		if ( plan == null ) {
			return;
		}
		plan.process();
	}

	@Override
	public void execute() {
		PojoIndexingPlan plan = sessionContext.getCurrentIndexingPlan( false );
		if ( plan == null ) {
			return;
		}
		sessionContext.getConfiguredAutomaticIndexingSynchronizationStrategy()
				.executeAndSynchronize( plan );
	}

	private <T> PojoRawTypeIdentifier<? extends T> getTypeIdentifier(T entity) {
		return sessionContext.getRuntimeIntrospector().getEntityTypeIdentifier( entity );
	}

	private <T> PojoRawTypeIdentifier<T> getTypeIdentifier(Class<T> entityType) {
		return typeContextProvider.getTypeIdentifierByJavaClass( entityType );
	}

	private PojoRawTypeIdentifier<?> getTypeIdentifier(String hibernateOrmEntityName) {
		return typeContextProvider.getTypeIdentifierByEntityName( hibernateOrmEntityName );
	}
}
