/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.AbstractSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;

public class StubSearchIndexScope
		extends AbstractSearchIndexScope<
						StubSearchIndexScope,
						StubIndexModel,
						StubSearchIndexNodeContext,
						StubSearchIndexCompositeNodeContext
				> {
	public StubSearchIndexScope(BackendMappingContext mappingContext, Set<StubIndexModel> indexModels) {
		super( mappingContext, indexModels );
	}

	@Override
	protected StubSearchIndexScope self() {
		return this;
	}

	@Override
	protected StubSearchIndexCompositeNodeContext createMultiIndexSearchRootContext(
			List<StubSearchIndexCompositeNodeContext> rootForEachIndex) {
		return new StubMultiIndexSearchIndexCompositeNodeContext( this, null,
				rootForEachIndex );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected StubSearchIndexNodeContext createMultiIndexSearchValueFieldContext(String absolutePath,
			List<StubSearchIndexNodeContext> fieldForEachIndex) {
		return new StubMultiIndexSearchIndexValueFieldContext<>( this, absolutePath,
				(List) fieldForEachIndex );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected StubSearchIndexNodeContext createMultiIndexSearchObjectFieldContext(String absolutePath,
			List<StubSearchIndexNodeContext> fieldForEachIndex) {
		return new StubMultiIndexSearchIndexCompositeNodeContext( this, absolutePath,
				(List) fieldForEachIndex );
	}
}
