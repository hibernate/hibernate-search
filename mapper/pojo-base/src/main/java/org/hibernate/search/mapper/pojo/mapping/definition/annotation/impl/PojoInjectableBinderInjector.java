/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public final class PojoInjectableBinderInjector {

	private final AnnotationPojoInjectableBinderCollector binderCollector;

	public PojoInjectableBinderInjector(PojoBootstrapIntrospector introspector) {
		this.binderCollector = new AnnotationPojoInjectableBinderCollector( introspector );
	}

	public AnnotationPojoInjectableBinderCollector binderCollector() {
		return binderCollector;
	}

	public void injectFields(PropertyBinder binder, IndexSchemaElement indexSchemaElement, NamedValues params) {
		var injectors = binderCollector.injector( binder.getClass() );

		for ( var injector : injectors ) {
			injector.inject( binder, indexSchemaElement, params );
		}
	}
}
