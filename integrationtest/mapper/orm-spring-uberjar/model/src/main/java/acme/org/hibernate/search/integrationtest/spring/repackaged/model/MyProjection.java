/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package acme.org.hibernate.search.integrationtest.spring.repackaged.model;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

public class MyProjection {
	public String name;

	@ProjectionConstructor
	public MyProjection(@FieldProjection(path = "name") String name) {
		this.name = name;
	}
}
