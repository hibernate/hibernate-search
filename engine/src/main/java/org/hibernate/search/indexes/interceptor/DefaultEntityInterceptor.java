/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.interceptor;

/**
 * Default interceptor logic:
 *
 * If the hierarchy does not define a specific interceptor, then no interception logic is applied
 * If the hierarchy defines a specific interceptor, then we inherit the explicit interceptor defined
 * by the most specific superclass and use it.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DefaultEntityInterceptor implements EntityIndexingInterceptor {

	String EXCEPTION_MESSAGE = "The default interceptor must not be called. This is an Hibernate Search bug.";

	@Override
	public IndexingOverride onAdd(Object entity) {
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}

	@Override
	public IndexingOverride onUpdate(Object entity) {
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}

	@Override
	public IndexingOverride onDelete(Object entity) {
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}

	@Override
	public IndexingOverride onCollectionUpdate(Object entity) {
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}
}
