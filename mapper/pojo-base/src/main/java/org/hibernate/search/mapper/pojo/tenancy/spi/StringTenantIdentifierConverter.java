/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.tenancy.spi;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.logging.impl.FormattingLog;
import org.hibernate.search.mapper.pojo.tenancy.TenantIdentifierConverter;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A simple string-string tenant identifier converter implementation
 * to support applications that were using string tenant identifiers with Hibernate Search.
 */
@Incubating
public class StringTenantIdentifierConverter implements TenantIdentifierConverter {

	public static final StringTenantIdentifierConverter INSTANCE = new StringTenantIdentifierConverter();

	public static final String NAME = "string-tenant-identifier-converter";

	@Override
	public String toStringValue(Object tenantId) {
		if ( tenantId == null ) {
			return null;
		}
		if ( !( tenantId instanceof CharSequence ) ) {
			throw FormattingLog.INSTANCE.nonStringTenantId( tenantId );
		}
		else {
			return Objects.toString( tenantId );
		}
	}

	@Override
	public Object fromStringValue(String tenantId) {
		return tenantId;
	}
}
