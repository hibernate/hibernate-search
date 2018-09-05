/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.interceptor;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

/**
 * Only index blog when it is in published state
 *
 * @author Emmanuel Bernard
 */
public class IndexWhenPublishedInterceptor implements EntityIndexingInterceptor<Blog> {
	@Override
	public IndexingOverride onAdd(Blog entity) {
		if ( entity.getStatus() == BlogStatus.PUBLISHED ) {
			return IndexingOverride.APPLY_DEFAULT;
		}
		return IndexingOverride.SKIP;
	}

	@Override
	public IndexingOverride onUpdate(Blog entity) {
		if ( entity.getStatus() == BlogStatus.PUBLISHED ) {
			return IndexingOverride.UPDATE;
		}
		return IndexingOverride.REMOVE;
	}

	@Override
	public IndexingOverride onDelete(Blog entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onCollectionUpdate(Blog entity) {
		return onUpdate( entity );
	}
}
