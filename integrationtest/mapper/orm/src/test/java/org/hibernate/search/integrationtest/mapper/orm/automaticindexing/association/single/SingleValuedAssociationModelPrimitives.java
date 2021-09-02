/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.single;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.AssociationModelPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.PropertyAccessor;

public interface SingleValuedAssociationModelPrimitives<TIndexed extends TContaining, TContaining, TContained>
		extends AssociationModelPrimitives<TIndexed, TContaining, TContained> {

	PropertyAccessor<TContaining, TContained> containedNonIndexedEmbedded();

	PropertyAccessor<TContained, TContaining> containingAsNonIndexedEmbedded();

}
