/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.loading.impl;

import java.util.Map;

import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.loading.impl.PojoLoadingPlan;
import org.hibernate.search.mapper.pojo.loading.impl.PojoMultiLoaderLoadingPlan;
import org.hibernate.search.mapper.pojo.loading.impl.PojoSingleLoaderLoadingPlan;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext;

public class PojoSearchLoadingContext<R, E> implements SearchLoadingContext<R, E> {
	private final Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName;
	private final DocumentReferenceConverter<R> documentReferenceConverter;
	private final BridgeSessionContext sessionContext;
	private final PojoLoadingContext delegate;

	public PojoSearchLoadingContext(
			Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName,
			DocumentReferenceConverter<R> documentReferenceConverter,
			BridgeSessionContext sessionContext,
			PojoLoadingContext delegate) {
		this.targetTypesByEntityName = targetTypesByEntityName;
		this.documentReferenceConverter = documentReferenceConverter;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public PojoLoadingContext unwrap() {
		return delegate;
	}

	@Override
	public ProjectionHitMapper<R, E> createProjectionHitMapper() {
		PojoLoadingPlan<E> loadingPlan;
		if ( hasCommonLoaderKey() ) {
			loadingPlan = new PojoSingleLoaderLoadingPlan<>( delegate );
		}
		else {
			loadingPlan = new PojoMultiLoaderLoadingPlan<>( delegate );
		}
		return new PojoProjectionHitMapper<>( targetTypesByEntityName, documentReferenceConverter, sessionContext,
				loadingPlan );
	}

	private boolean hasCommonLoaderKey() {
		Object loaderKey = null;
		for ( PojoSearchLoadingIndexedTypeContext<? extends E> typeContext : targetTypesByEntityName.values() ) {
			Object thisTypeLoaderKey = delegate.loaderKey( typeContext.typeIdentifier() );
			if ( loaderKey == null ) {
				loaderKey = thisTypeLoaderKey;
			}
			else if ( !loaderKey.equals( thisTypeLoaderKey ) ) {
				return false;
			}
		}
		return true;
	}
}
