/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.typebridge.ormcontext;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;

@Entity
@Indexed
@TypeBinding(binder = @TypeBinderRef(type = MyEntityTypeBinder.class))
public class MyEntity {

	@Id
	@GeneratedValue
	private Integer id;

	public Integer getId() {
		return id;
	}
}
