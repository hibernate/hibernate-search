/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.string;

import java.io.Serializable;

import org.hibernate.annotations.Immutable;

@Immutable
public class ISBN implements Serializable {

	private final Long id;

	public ISBN(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ISBN && id.equals( ( (ISBN) obj ).id );
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
