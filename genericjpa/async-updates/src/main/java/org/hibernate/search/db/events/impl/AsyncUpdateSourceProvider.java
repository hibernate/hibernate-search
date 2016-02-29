/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.impl;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.db.util.impl.EntityManagerFactoryWrapper;


/**
 * @author Martin Braun
 */
public interface AsyncUpdateSourceProvider {

	AsyncUpdateSource getUpdateSource(
			long delay,
			TimeUnit timeUnit,
			int batchSizeForUpdates,
			Properties properties,
			EntityManagerFactoryWrapper emf);

}
