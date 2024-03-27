/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.reflect.Member;

class HibernateOrmBasicClassPropertyMetadata {
	private final Member member;
	private final boolean id;

	HibernateOrmBasicClassPropertyMetadata(Member member, boolean id) {
		this.member = member;
		this.id = id;
	}

	Member getMember() {
		return member;
	}

	boolean isId() {
		return id;
	}
}
