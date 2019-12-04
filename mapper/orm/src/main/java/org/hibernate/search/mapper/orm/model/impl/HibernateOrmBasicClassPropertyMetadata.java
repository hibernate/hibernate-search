/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
