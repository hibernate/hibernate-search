/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.selection;

import jakarta.inject.Singleton;

import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;

// tag::include[]
@Singleton
public class MyLoadingBinder implements EntityLoadingBinder { // <1>
	@Override
	public void bind(EntityLoadingBindingContext context) { // <2>
		context.selectionLoadingStrategy( // <3>
				Book.class, // <4>
				new MySelectionLoadingStrategy<>( Book.class ) // <5>
		);
	}
}
// end::include[]
