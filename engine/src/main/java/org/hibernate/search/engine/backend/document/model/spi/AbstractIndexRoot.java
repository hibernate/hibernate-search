/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractIndexRoot<
		S extends AbstractIndexRoot<S, SC, NT, F>,
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexCompositeNodeType<SC, ? super S>,
		F extends IndexField<SC, ?>>
		extends AbstractIndexNode<S, SC, NT>
		implements IndexCompositeNode<SC, NT, F> {

	private final Map<String, F> staticChildrenByName;

	public AbstractIndexRoot(NT type, Map<String, F> notYetInitializedStaticChildren) {
		super( type );
		// We expect the children to be added to the list externally, just after the constructor call.
		this.staticChildrenByName = Collections.unmodifiableMap( notYetInitializedStaticChildren );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[type=" + type + "]";
	}

	@Override
	public final EventContext relativeEventContext() {
		return EventContexts.indexSchemaRoot();
	}

	@Override
	public final boolean isComposite() {
		return true;
	}

	@Override
	public final boolean isRoot() {
		return true;
	}

	@Override
	public final boolean isObjectField() {
		return false;
	}

	@Override
	public final boolean isValueField() {
		return false;
	}

	@Override
	public S toComposite() {
		return self();
	}

	@Override
	public IndexObjectField<SC, ?, ?, ?> toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public final String absolutePath() {
		return null;
	}

	@Override
	public final String absolutePath(String relativeFieldName) {
		return relativeFieldName;
	}

	@Override
	public final String[] absolutePathComponents() {
		return new String[0];
	}

	@Override
	public final TreeNodeInclusion inclusion() {
		return TreeNodeInclusion.INCLUDED;
	}

	@Override
	public final List<String> nestedPathHierarchy() {
		return Collections.emptyList();
	}

	@Override
	public final Map<String, F> staticChildrenByName() {
		return staticChildrenByName;
	}

	@Override
	public final boolean multiValued() {
		return false;
	}

	@Override
	public boolean multiValuedInRoot() {
		return false;
	}

	@Override
	public final String closestMultiValuedParentAbsolutePath() {
		return null;
	}

	@Override
	final SearchIndexSchemaElementContextHelper helper() {
		return SearchIndexSchemaElementContextHelper.COMPOSITE;
	}

}
