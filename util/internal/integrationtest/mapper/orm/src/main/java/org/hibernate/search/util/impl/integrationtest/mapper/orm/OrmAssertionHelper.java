/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.common.assertion.MappingAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;

public class OrmAssertionHelper extends MappingAssertionHelper<EntityManagerFactory> {
	public OrmAssertionHelper(BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
	}

	@Override
	protected void doRefresh(EntityManagerFactory entryPoint) {
		Search.mapping( entryPoint ).scope( Object.class ).workspace().refresh();
	}
}
