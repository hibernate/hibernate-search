/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;

public interface HibernateOrmMassIndexingSessionContext {

	SessionImplementor session();

	PojoIndexer createIndexer();

	PojoRuntimeIntrospector runtimeIntrospector();

	EntityReferenceFactory<EntityReference> entityReferenceFactory();

}
