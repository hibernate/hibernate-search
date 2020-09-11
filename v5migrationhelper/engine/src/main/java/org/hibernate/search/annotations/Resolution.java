/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

/**
 * Date indexing resolution.
 *
 * @author Emmanuel Bernard
 * @deprecated {@link DateBridge}/{@link CalendarBridge} are no longer available in Hibernate Search 6.
 * See the javadoc of {@link DateBridge} or {@link CalendarBridge}.
 */
@Deprecated
public enum Resolution {
	YEAR,
	MONTH,
	DAY,
	HOUR,
	MINUTE,
	SECOND,
	MILLISECOND
}
