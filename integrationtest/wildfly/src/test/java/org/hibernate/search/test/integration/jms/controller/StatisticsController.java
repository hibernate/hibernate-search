/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms.controller;

import javax.ejb.Stateless;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Accessor to the {@link SessionFactory} statistics.
 *
 * @author Davide D'Alto
 */
@Stateless
public class StatisticsController {

	@PersistenceUnit
	private EntityManagerFactory factory;

	public Statistics getStatistics() {
		return factory.unwrap( SessionFactory.class ).getStatistics();
	}
}
