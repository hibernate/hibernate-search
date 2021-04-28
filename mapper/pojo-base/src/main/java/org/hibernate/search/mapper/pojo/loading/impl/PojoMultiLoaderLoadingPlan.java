/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;

public final class PojoMultiLoaderLoadingPlan<T> implements PojoLoadingPlan<T> {

	private final PojoSelectionLoadingContext context;

	private final Map<PojoLoadingTypeContext<? extends T>, PojoSingleLoaderLoadingPlan<T>> planByType = new LinkedHashMap<>();
	private final Map<Object, PojoSingleLoaderLoadingPlan<T>> planByLoaderKey = new LinkedHashMap<>();

	public PojoMultiLoaderLoadingPlan(PojoSelectionLoadingContext context) {
		this.context = context;
	}

	@Override
	public int planLoading(PojoLoadingTypeContext<? extends T> expectedType, Object identifier) {
		return delegate( expectedType ).planLoading( expectedType, identifier );
	}

	@Override
	public void loadBlocking(Deadline deadline) {
		context.checkOpen();
		for ( PojoSingleLoaderLoadingPlan<T> delegate : planByLoaderKey.values() ) {
			delegate.loadBlocking( deadline );
		}
	}

	@Override
	public <T2 extends T> T2 retrieve(PojoLoadingTypeContext<T2> expectedType, int ordinal) {
		return delegate( expectedType ).retrieve( expectedType, ordinal );
	}

	@Override
	public void clear() {
		planByType.clear();
		planByLoaderKey.clear();
	}

	private PojoSingleLoaderLoadingPlan<T> delegate(PojoLoadingTypeContext<? extends T> type) {
		PojoSingleLoaderLoadingPlan<T> delegate = planByType.get( type );
		if ( delegate != null ) {
			return delegate;
		}

		Object loaderKey = context.loaderKey( type );
		delegate = planByLoaderKey.get( loaderKey );
		if ( delegate != null ) {
			planByType.put( type, delegate );
			return delegate;
		}

		delegate = new PojoSingleLoaderLoadingPlan<>( context );
		planByType.put( type, delegate );
		planByLoaderKey.put( loaderKey, delegate );
		return delegate;
	}
}
