/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.Set;

import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;

/**
 * Contextual information about a mass indexing proccess.
 */
public interface PojoMassIndexingContext extends PojoMassLoadingContext {

	Set<String> tenantIds();

	TenancyMode tenancyMode();

	MassIndexingDefaultCleanOperation massIndexingDefaultCleanOperation();
}
