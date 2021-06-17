/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.AbstractMultiIndexSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

public class StubMultiIndexSearchIndexValueFieldContext<F>
		extends AbstractMultiIndexSearchIndexValueFieldContext<
						StubSearchIndexValueFieldContext<F>,
						StubSearchIndexScope,
						StubSearchIndexValueFieldTypeContext<F>,
						F
				>
		implements StubSearchIndexValueFieldContext<F>, StubSearchIndexValueFieldTypeContext<F> {

	public StubMultiIndexSearchIndexValueFieldContext(StubSearchIndexScope scope, String absolutePath,
			List<? extends StubSearchIndexValueFieldContext<F>> fieldForEachIndex) {
		super( scope, absolutePath, fieldForEachIndex );
	}

	@Override
	protected StubSearchIndexValueFieldContext<F> self() {
		return this;
	}

	@Override
	protected StubSearchIndexValueFieldTypeContext<F> selfAsNodeType() {
		return this;
	}

	@Override
	protected StubSearchIndexValueFieldTypeContext<F> typeOf(StubSearchIndexValueFieldContext<F> indexElement) {
		return indexElement.type();
	}

	@Override
	public StubSearchIndexCompositeNodeContext toComposite() {
		return SearchIndexSchemaElementContextHelper.throwingToComposite( this );
	}
}
