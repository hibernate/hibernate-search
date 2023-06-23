/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface IndexField<
		SC extends SearchIndexScope<?>,
		C extends IndexCompositeNode<SC, ?, ?>>
		extends IndexNode<SC>, IndexFieldDescriptor {

	@Override
	C toComposite();

	@Override
	IndexObjectField<SC, ?, C, ?> toObjectField();

	@Override
	IndexValueField<SC, ?, C> toValueField();

	@Override
	C parent();

}
