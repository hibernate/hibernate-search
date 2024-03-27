/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.ormcontext;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

@Entity
@Indexed
public class MyEntity {

	@Id
	@GeneratedValue
	private Integer id;

	@PropertyBinding(binder = @PropertyBinderRef(type = MyDataPropertyBinder.class))
	private MyData myData;

	public Integer getId() {
		return id;
	}

	public MyData getMyData() {
		return myData;
	}

	public void setMyData(MyData myData) {
		this.myData = myData;
	}
}
