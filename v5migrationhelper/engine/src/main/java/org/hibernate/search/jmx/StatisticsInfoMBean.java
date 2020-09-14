/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id:$
package org.hibernate.search.jmx;

import org.hibernate.search.stat.Statistics;

/**
 * @author Hardy Ferentschik
 */
public interface StatisticsInfoMBean extends Statistics {

	String STATISTICS_MBEAN_OBJECT_NAME = "org.hibernate.search.jmx:type=StatisticsInfoMBean";

}
