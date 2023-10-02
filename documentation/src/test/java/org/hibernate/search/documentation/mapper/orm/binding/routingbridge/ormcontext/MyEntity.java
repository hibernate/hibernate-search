/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.routingbridge.ormcontext;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed(routingBinder = @RoutingBinderRef(type = MyEntityRoutingBinder.class))
public class MyEntity {

	@Id
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
