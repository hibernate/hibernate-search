/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
