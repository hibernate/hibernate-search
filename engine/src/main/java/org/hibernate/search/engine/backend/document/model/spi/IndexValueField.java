/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexValueFieldType;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldContext;

public interface IndexValueField<
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexValueFieldType<SC, ?, ?>,
		C extends IndexCompositeNode<SC, ?, ?>>
		extends IndexNode<SC>, IndexField<SC, C>, IndexValueFieldDescriptor,
		SearchIndexValueFieldContext<SC> {

	@Override
	NT type();

}
