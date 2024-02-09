/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.spi;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingSessionContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

public interface HibernateOrmLoadingSessionContext extends PojoLoadingSessionContext, PojoMassIndexingSessionContext {

	SessionImplementor session();

}
