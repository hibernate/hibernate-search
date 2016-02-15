/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa;

import javax.persistence.EntityManager;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer;
import org.hibernate.search.jpa.FullTextEntityManager;

/**
 * This interface is the main entry point to get Search working in your JPA application
 *
 * @author Martin Braun
 */
public interface JPASearchFactoryController {

	/**
	 * @return the underlying SearchFactory
	 */
	SearchFactory getSearchFactory();

	/**
	 * (un-)pauses updating if possible
	 */
	void pauseUpdating(boolean pause);

	/**
	 * @param em may be null if you only want to do index related operations
	 */
	FullTextEntityManager getFullTextEntityManager(EntityManager em);

	/**
	 * used to register UpdateConsumers to do manual update processing
	 */
	void addUpdateConsumer(UpdateConsumer updateConsumer);

	void removeUpdateConsumer(UpdateConsumer updateConsumer);

	/**
	 * closes this Controller and all underlying resources
	 */
	void close();

}
