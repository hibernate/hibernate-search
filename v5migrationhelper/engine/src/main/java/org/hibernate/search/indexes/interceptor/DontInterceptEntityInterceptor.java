/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.interceptor;

/**
 * Do not apply any interception.
 * Useful to force a subclass not to inherit its superclass interceptor.
 *
 * @author Emmanuel Bernard
 */
public class DontInterceptEntityInterceptor implements EntityIndexingInterceptor {

	@Override
	public IndexingOverride onAdd(Object entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onUpdate(Object entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onDelete(Object entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onCollectionUpdate(Object entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}
}
