/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.loading.spi.ReferenceHitMapper;
import org.hibernate.search.mapper.orm.common.EntityReference;

public interface HibernateOrmLoadingSessionContext {

	SessionImplementor getSession();

	ReferenceHitMapper<EntityReference> getReferenceHitMapper();

}
