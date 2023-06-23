/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.util.List;

import org.hibernate.search.engine.backend.types.spi.AbstractIndexValueFieldType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldContext;

public abstract class AbstractIndexValueField<
		S extends AbstractIndexValueField<S, SC, FT, C, F>,
		SC extends SearchIndexScope<?>,
		FT extends AbstractIndexValueFieldType<SC, ? super S, F>,
		C extends IndexCompositeNode<SC, ?, ?>,
		F>
		extends AbstractIndexField<S, SC, FT, C>
		implements IndexValueField<SC, FT, C>, SearchIndexValueFieldContext<SC> {

	public AbstractIndexValueField(C parent, String relativeFieldName, FT type, TreeNodeInclusion inclusion,
			boolean multiValued) {
		super( parent, relativeFieldName, type, inclusion, multiValued );
	}

	@Override
	public final boolean isComposite() {
		return false;
	}

	@Override
	public final boolean isObjectField() {
		return false;
	}

	@Override
	public final boolean isValueField() {
		return true;
	}

	@Override
	public final C toComposite() {
		return SearchIndexSchemaElementContextHelper.throwingToComposite( this );
	}

	@Override
	public IndexObjectField<SC, ?, C, ?> toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public final S toValueField() {
		return self();
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return parent.nestedPathHierarchy();
	}

	@Override
	final SearchIndexSchemaElementContextHelper helper() {
		return SearchIndexSchemaElementContextHelper.VALUE_FIELD;
	}
}
