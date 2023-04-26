/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubSearchLoadingContext implements SearchLoadingContext<DocumentReference> {

	StubSearchLoadingContext() {
	}

	@Override
	public Object unwrap() {
		return this;
	}

	@Override
	public ProjectionHitMapper<DocumentReference> createProjectionHitMapper() {
		return new StubProjectionHitMapper();
	}
}
