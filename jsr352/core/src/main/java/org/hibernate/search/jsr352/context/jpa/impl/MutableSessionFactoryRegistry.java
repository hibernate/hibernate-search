/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.context.jpa.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.jsr352.context.jpa.spi.EntityManagerFactoryRegistry;


/**
 * @author Yoann Rodiere
 */
public interface MutableSessionFactoryRegistry extends EntityManagerFactoryRegistry {

	void register(SessionFactoryImplementor sessionFactory);

	void unregister(SessionFactoryImplementor sessionFactory);

}
