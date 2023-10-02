/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.tenancy;

import java.util.Objects;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Converts the tenant identifier value to a string that can be used for storing in the index.
 * Converts the string representation of a tenant identifier when an object representation of it is required,
 * e.g. opening a new session to get entities etc.
 */
@Incubating
public interface TenantIdentifierConverter {

	/**
	 * Converts an object representation of the tenant identifier to a string representation.
	 * @param tenantId The tenant identifier to convert to a string. May be {@code null}.
	 * @return A string representation of the tenant identifier.
	 */
	default String toStringValue(Object tenantId) {
		return Objects.toString( tenantId, null );
	}

	/**
	 * Converts a string representation of the tenant identifier to an object representation.
	 * @param tenantId The tenant identifier to convert back to an object representation. May be {@code null}.
	 * @return An Object representation of the tenant identifier.
	 */
	Object fromStringValue(String tenantId);

}
