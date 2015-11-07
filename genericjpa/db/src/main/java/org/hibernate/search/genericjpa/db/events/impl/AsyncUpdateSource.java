/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.impl;

import java.util.List;

import org.hibernate.search.genericjpa.db.events.UpdateConsumer;

/**
 * Source for updates on entities. This does no hierarchy checks, it just delivers information about which entry in
 * which table has changed
 *
 * @author Martin Braun
 */
public interface AsyncUpdateSource {

	void setUpdateConsumers(List<UpdateConsumer> updateConsumers);

	void start();

	/**
	 * this has to wait until the current jobs are finished
	 */
	void stop();

	/**
	 * this may wait until the current jobs are finished
	 */
	void pause(boolean pause);

}
