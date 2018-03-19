/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

/**
 * A service that can be available in a {@link Library}.
 */
public enum LibraryService {

	READING_ROOMS,
	HARDCOPY_LOAN,
	DEMATERIALIZED_LOAN,
	DISABLED_ACCESS

}
