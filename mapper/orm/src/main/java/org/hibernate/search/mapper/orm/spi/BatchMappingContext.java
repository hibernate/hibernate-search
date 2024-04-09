/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.spi;

import java.util.Collection;

import jakarta.persistence.EntityManager;

import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;

public interface BatchMappingContext {

	BatchTypeContextProvider typeContextProvider();

	BatchSessionContext sessionContext(EntityManager entityManager);

	<T> BatchScopeContext<T> scope(Class<T> expectedSuperType);

	<T> BatchScopeContext<T> scope(Class<T> expectedSuperType, String entityName);

	<T> BatchScopeContext<T> scope(Collection<? extends Class<? extends T>> classes);

	TenancyConfiguration tenancyConfiguration();

}
