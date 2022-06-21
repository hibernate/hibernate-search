/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.scope.impl;

import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;

public interface StandalonePojoScopeSessionContext
		extends PojoScopeSessionContext, StandalonePojoMassIndexingSessionContext {

	DocumentReferenceConverter<EntityReference> documentReferenceConverter();

}
