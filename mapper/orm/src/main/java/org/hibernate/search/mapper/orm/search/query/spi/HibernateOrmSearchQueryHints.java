/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.spi;

/**
 * Constants for query hints accepted by Hibernate Search.
 * <p>
 * We redefine the constants here instead of using those exposed by Hibernate ORM,
 * because the constants from Hibernate ORM are not transformed currently
 * in some versions of the Jakarta artifacts (they start with "javax.persistence." instead of "jakarta.persistence.").
 * By defining the constants directly in our project, we can transform the constants correctly
 * in our own Jakarta artifacts.
 */
public final class HibernateOrmSearchQueryHints {
	private HibernateOrmSearchQueryHints() {
	}

	public static final String TIMEOUT_JPA = "javax.persistence.query.timeout";
	public static final String TIMEOUT_HIBERNATE = "org.hibernate.timeout";
	public static final String FETCHGRAPH = "javax.persistence.fetchgraph";
	public static final String LOADGRAPH = "javax.persistence.loadgraph";
}
