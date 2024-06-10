/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.dto;

import java.util.List;

import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

@ProjectionConstructor
public class LibrarySimpleProjection {

	public final String name;
	public final List<LibraryServiceOption> services;

	public LibrarySimpleProjection(String name, List<LibraryServiceOption> services) {
		this.name = name;
		this.services = services;
	}

}
