/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

public enum IndexFieldStructure {
	/**
	 * The field is at the document root.
	 */
	ROOT,
	/**
	 * The field is in a flattened object field.
	 */
	IN_FLATTENED,
	/**
	 * The field is in a nested object field (nested document).
	 */
	IN_NESTED,
	/**
	 * The field is in a nested object field (nested document)
	 * which itself is in a nested object field (nested document).
	 */
	IN_NESTED_TWICE;
}
