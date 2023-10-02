/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.mass;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastore;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;

// tag::include[]
@Singleton
public class MyLoadingBinder implements EntityLoadingBinder { // <1>
	private final MyDatastore datastore;

	@Inject // <2>
	public MyLoadingBinder(MyDatastore datastore) {
		this.datastore = datastore;
	}

	@Override
	public void bind(EntityLoadingBindingContext context) { // <3>
		context.massLoadingStrategy( // <4>
				Book.class, // <5>
				new MyMassLoadingStrategy<>( datastore, Book.class ) // <6>
		);
	}
}
// end::include[]
