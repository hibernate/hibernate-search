/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.io.Serializable;

import org.hibernate.annotations.Immutable;

@Immutable
public class ISBN implements Serializable {
	private final String stringValue;

	public ISBN(String stringValue) {
		this.stringValue = stringValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ISBN && stringValue.equals( ( (ISBN) obj ).stringValue );
	}

	@Override
	public int hashCode() {
		return stringValue.hashCode();
	}
}
