/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.scope.impl;

import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;

public interface JavaBeanScopeSessionContext
		extends PojoScopeSessionContext, JavaBeanMassIndexingSessionContext {

	DocumentReferenceConverter<EntityReference> documentReferenceConverter();

}
