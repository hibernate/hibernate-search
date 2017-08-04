/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;

import java.util.Collection;

/**
 * @author Yoann Rodiere
 */
public interface IndexableTypeOrdering {

	boolean isSubType(IndexedTypeIdentifier parent, IndexedTypeIdentifier subType);

	Collection<? extends IndexedTypeIdentifier> getAscendingSuperTypes(IndexedTypeIdentifier subType);

	Collection<? extends IndexedTypeIdentifier> getDescendingSuperTypes(IndexedTypeIdentifier subType);

}
