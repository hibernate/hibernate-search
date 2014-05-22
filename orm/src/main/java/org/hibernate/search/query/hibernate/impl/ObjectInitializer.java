/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * Initializes a set of objects from EntityInfos
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface ObjectInitializer {
	void initializeObjects(EntityInfo[] entityInfos,
						   Criteria criteria,
						   Class<?> entityType,
						   SearchFactoryImplementor searchFactoryImplementor,
						   TimeoutManager timeoutManager,
						   Session session);
}
