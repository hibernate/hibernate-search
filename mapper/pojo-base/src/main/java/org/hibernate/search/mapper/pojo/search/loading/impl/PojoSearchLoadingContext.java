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
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;

public class PojoSearchLoadingContext<R, E> implements SearchLoadingContext<R, E> {
	private final Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName;
	private final DocumentReferenceConverter<R> documentReferenceConverter;
	private final BridgeSessionContext sessionContext;
	private final PojoSelectionLoadingContext delegate;

	public PojoSearchLoadingContext(
			Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName,
			DocumentReferenceConverter<R> documentReferenceConverter,
			BridgeSessionContext sessionContext,
			PojoSelectionLoadingContext delegate) {
		this.targetTypesByEntityName = targetTypesByEntityName;
		this.documentReferenceConverter = documentReferenceConverter;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public PojoSelectionLoadingContext unwrap() {
		return delegate;
	}

	@Override
	public ProjectionHitMapper<R, E> createProjectionHitMapper() {
		PojoLoadingPlan<E> loadingPlan = PojoLoadingPlan.create( delegate, targetTypesByEntityName.values() );
		return new PojoProjectionHitMapper<>( targetTypesByEntityName, documentReferenceConverter, sessionContext,
				loadingPlan );
	}
}
