/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.impl;

import java.util.List;

/**
 * EventModelParsers are used to find out in which Tables entities are stored.
 *
 * Specific implementations might rely on Annotations to be present or try to parse Metadata
 * received from the JPA provider
 *
 * These {@link EventModelInfo}s are then used during Update table generation as well as
 * while querying for Updates in the Update tables
 *
 * @author Martin Braun
 */
public interface EventModelParser {

	String DEFAULT_HSEARCH_UPDATES_SUFFIX = "hsearchupdates";
	String DEFAULT_EVENT_TYPE_COLUMN = DEFAULT_HSEARCH_UPDATES_SUFFIX + "eventType" + DEFAULT_HSEARCH_UPDATES_SUFFIX;
	String DEFAULT_UPDATE_ID_COLUMN = "hs" + "updateId" + DEFAULT_HSEARCH_UPDATES_SUFFIX;

	List<EventModelInfo> parse(List<Class<?>> entities);

}
