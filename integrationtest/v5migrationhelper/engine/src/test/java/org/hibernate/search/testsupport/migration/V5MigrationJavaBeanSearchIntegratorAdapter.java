/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.migration;

import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.common.impl.CollectionHelper;

public class V5MigrationJavaBeanSearchIntegratorAdapter implements SearchIntegrator {
	private final SearchMapping delegate;

	public V5MigrationJavaBeanSearchIntegratorAdapter(SearchMapping delegate) {
		this.delegate = delegate;
	}

	@Override
	public V5MigrationSearchScope scope(Class<?>... targetTypes) {
		return new V5MigrationJavaBeanSearchScopeAdapter( delegate.scope( CollectionHelper.asSet( targetTypes ) ) );
	}
}
