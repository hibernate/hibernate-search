/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public abstract class AbstractIndexObjectField<
		S extends AbstractIndexObjectField<S, SC, NT, C, F>,
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexCompositeNodeType<SC, ? super S>,
		C extends IndexCompositeNode<SC, NT, F>,
		F extends IndexField<SC, ?>>
		extends AbstractIndexField<S, SC, NT, C>
		implements IndexObjectField<SC, NT, C, F> {

	private final List<String> nestedPathHierarchy;
	private final Map<String, F> staticChildrenByName;

	public AbstractIndexObjectField(C parent, String relativeFieldName,
			NT type, TreeNodeInclusion inclusion, boolean multiValued,
			Map<String, F> notYetInitializedStaticChildren) {
		super( parent, relativeFieldName, type, inclusion, multiValued );
		// at the root object level the nestedPathHierarchy is empty
		List<String> theNestedPathHierarchy = parent.nestedPathHierarchy();
		if ( type.nested() ) {
			// if we found a nested object, we add it to the nestedPathHierarchy
			theNestedPathHierarchy = new ArrayList<>( theNestedPathHierarchy );
			theNestedPathHierarchy.add( absolutePath );
		}
		this.nestedPathHierarchy = Collections.unmodifiableList( theNestedPathHierarchy );
		// We expect the children to be added to the list externally, just after the constructor call.
		this.staticChildrenByName = Collections.unmodifiableMap( notYetInitializedStaticChildren );
	}

	@Override
	public final boolean isRoot() {
		return false;
	}

	@Override
	public final boolean isComposite() {
		return true;
	}

	@Override
	public final boolean isObjectField() {
		return true;
	}

	@Override
	public final boolean isValueField() {
		return false;
	}

	@Override
	public final S toObjectField() {
		return self();
	}

	@Override
	public IndexValueField<SC, ?, C> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

	@Override
	public final String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath(), relativeFieldName );
	}

	@Override
	final SearchIndexSchemaElementContextHelper helper() {
		return SearchIndexSchemaElementContextHelper.COMPOSITE;
	}

	@Override
	public final List<String> nestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	public final Map<String, F> staticChildrenByName() {
		return staticChildrenByName;
	}

}
