/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface IndexNode<SC extends SearchIndexScope<?>>
		extends SearchIndexNodeContext<SC> {

	@Override
	IndexCompositeNode<SC, ?, ?> toComposite();

	@Override
	IndexObjectField<SC, ?, ?, ?> toObjectField();

	@Override
	IndexValueField<SC, ?, ?> toValueField();

	TreeNodeInclusion inclusion();

}
