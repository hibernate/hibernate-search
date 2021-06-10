/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.List;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;

public abstract class AbstractMultiIndexSearchIndexCompositeNodeContext<
				S extends SearchIndexCompositeNodeContext<SC>,
				SC extends SearchIndexScope<SC>,
				NT extends SearchIndexCompositeNodeTypeContext<SC, S>
		>
		extends AbstractMultiIndexSearchIndexNodeContext<S, SC, NT>
		implements SearchIndexCompositeNodeContext<SC>, SearchIndexCompositeNodeTypeContext<SC, S> {
	public AbstractMultiIndexSearchIndexCompositeNodeContext(SC scope, String absolutePath,
			List<? extends S> nodeForEachIndex) {
		super( scope, absolutePath, nodeForEachIndex );
	}

	@Override
	public final NT type() {
		return selfAsNodeType();
	}

	@Override
	public final boolean isComposite() {
		return true;
	}

	@Override
	public final boolean isValueField() {
		return false;
	}

	@Override
	public final S toComposite() {
		return self();
	}

	@Override
	public SearchIndexValueFieldContext<SC> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

	@Override
	public final String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath(), relativeFieldName );
	}

	@Override
	public final boolean nested() {
		return fromTypeIfCompatible( SearchIndexCompositeNodeTypeContext::nested, Object::equals,
				"nested" );
	}

	@Override
	final SearchIndexSchemaElementContextHelper helper() {
		return SearchIndexSchemaElementContextHelper.COMPOSITE;
	}
}
