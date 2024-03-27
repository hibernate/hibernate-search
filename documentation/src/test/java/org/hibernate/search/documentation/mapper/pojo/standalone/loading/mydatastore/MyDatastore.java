/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore;

import java.util.Map;

public class MyDatastore {

	final Map<Class<?>, Map<String, ?>> entities;

	public MyDatastore(Map<Class<?>, Map<String, ?>> entities) {
		this.entities = entities;
	}

	public MyDatastoreConnection connect() {
		return new MyDatastoreConnection( this );
	}

}
