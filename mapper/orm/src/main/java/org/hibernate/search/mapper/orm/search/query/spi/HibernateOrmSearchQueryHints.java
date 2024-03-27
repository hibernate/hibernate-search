/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.search.query.spi;

/**
 * Constants for query hints accepted by Hibernate Search.
 * <p>
 * We redefine the constants here instead of using those exposed by Hibernate ORM,
 * because the constants from Hibernate ORM are not compile-time constants:
 * some of them are initialized during static class initialization,
 * which prevents their use in switch constructs, in particular.
 */
public final class HibernateOrmSearchQueryHints {
	private HibernateOrmSearchQueryHints() {
	}

	// Don't remove the string concatenations:
	// they're hacks to avoid automated replacements when building some artifacts.
	private static final String JAVAX_PREFIX = "javax" + ".persistence.";
	private static final String JAKARTA_PREFIX = "jakarta" + ".persistence.";
	private static final String HIBERNATE_PREFIX = "org.hibernate.";

	public static final String JAVAX_TIMEOUT = JAVAX_PREFIX + "query.timeout";
	public static final String JAKARTA_TIMEOUT = JAKARTA_PREFIX + "query.timeout";
	public static final String HIBERNATE_TIMEOUT = HIBERNATE_PREFIX + "timeout";
	public static final String JAVAX_FETCHGRAPH = JAVAX_PREFIX + "fetchgraph";
	public static final String JAKARTA_FETCHGRAPH = JAKARTA_PREFIX + "fetchgraph";
	public static final String JAVAX_LOADGRAPH = JAVAX_PREFIX + "loadgraph";
	public static final String JAKARTA_LOADGRAPH = JAKARTA_PREFIX + "loadgraph";
}
