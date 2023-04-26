/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkMappingContext;

/**
 * Mapping-scoped information and operations for use in POJO scopes.
 */
public interface PojoScopeMappingContext extends PojoWorkMappingContext, PojoMassIndexingMappingContext {

	PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate();

}
