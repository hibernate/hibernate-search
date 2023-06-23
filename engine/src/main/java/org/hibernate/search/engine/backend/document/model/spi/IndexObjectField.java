/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface IndexObjectField<
		SC extends SearchIndexScope<?>,
		NT extends AbstractIndexCompositeNodeType<SC, ?>,
		C extends IndexCompositeNode<SC, NT, F>,
		F extends IndexField<SC, ?>>
		extends IndexNode<SC>, IndexField<SC, C>, IndexCompositeNode<SC, NT, F>,
		IndexObjectFieldDescriptor {

}
