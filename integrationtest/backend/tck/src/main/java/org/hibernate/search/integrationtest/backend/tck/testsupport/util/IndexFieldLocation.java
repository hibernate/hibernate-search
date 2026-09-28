package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

public enum IndexFieldLocation {
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
