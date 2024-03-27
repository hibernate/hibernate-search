/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.model;

/**
 * A service that can be available in a {@link Library}.
 */
public enum LibraryServiceOption {

	READING_ROOMS,
	HARDCOPY_LOAN,
	DEMATERIALIZED_LOAN,
	DISABLED_ACCESS

}
