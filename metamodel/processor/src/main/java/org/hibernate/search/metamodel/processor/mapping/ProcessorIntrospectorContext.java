/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.mapping;

import java.util.Map;

import javax.lang.model.element.TypeElement;

import org.hibernate.search.metamodel.processor.impl.HibernateSearchMetamodelProcessorContext;

public record ProcessorIntrospectorContext( HibernateSearchMetamodelProcessorContext processorContext,
											Map<String, TypeElement> typeElementsByName) {
}
