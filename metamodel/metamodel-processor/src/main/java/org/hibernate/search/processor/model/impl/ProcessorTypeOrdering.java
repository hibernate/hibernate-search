/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model.impl;

import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;

import org.hibernate.search.processor.impl.HibernateSearchMetamodelProcessorContext;
import org.hibernate.search.util.common.reflect.spi.AbstractTypeOrdering;

public class ProcessorTypeOrdering extends AbstractTypeOrdering<TypeElement> {

	private final HibernateSearchMetamodelProcessorContext context;

	public ProcessorTypeOrdering(HibernateSearchMetamodelProcessorContext context) {
		this.context = context;
	}

	@Override
	protected TypeElement superClass(TypeElement subType) {
		return (TypeElement) context.typeUtils().asElement( subType.getSuperclass() );
	}

	@Override
	protected Stream<TypeElement> declaredInterfaces(TypeElement subType) {
		return subType.getInterfaces().stream()
				.map( m -> (TypeElement) context.typeUtils().asElement( m ) );
	}
}
