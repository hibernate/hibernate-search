/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.function.Function;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubLoadingContext implements LoadingContext<DocumentReference, DocumentReference> {
	private final ProjectionHitMapper<DocumentReference, DocumentReference> projectionHitMapper;

	StubLoadingContext() {
		this.projectionHitMapper = new DefaultProjectionHitMapper<>( Function.identity(), EntityLoader.identity() );
	}

	@Override
	public ProjectionHitMapper<DocumentReference, DocumentReference> getProjectionHitMapper() {
		return projectionHitMapper;
	}
}
