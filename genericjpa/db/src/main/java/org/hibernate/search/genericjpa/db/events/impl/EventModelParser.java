/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.impl;

import java.util.List;
import java.util.Set;

/**
 * Created by Martin on 16.07.2015.
 */
public interface EventModelParser {

	List<EventModelInfo> parse(Set<Class<?>> updateClasses);

	List<EventModelInfo> parse(List<Class<?>> updateClasses);

}
