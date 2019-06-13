/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

public interface LuceneCompositeProjection<E, P> extends LuceneSearchProjection<E, P> {

	@Override
	default Set<String> getIndexNames() {
		// TODO handle composite in a subsequent commit
		return null;
	}

}
