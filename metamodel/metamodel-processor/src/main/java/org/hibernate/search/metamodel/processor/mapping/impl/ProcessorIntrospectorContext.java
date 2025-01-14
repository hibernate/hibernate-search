/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.mapping.impl;

import java.util.Objects;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.hibernate.search.metamodel.processor.annotation.processing.impl.ProcessorAnnotationProcessorContext;
import org.hibernate.search.metamodel.processor.impl.HibernateSearchMetamodelProcessorContext;

public final class ProcessorIntrospectorContext {
	private final HibernateSearchMetamodelProcessorContext processorContext;

	private String typeName;
	private TypeElement indexedEntityType;
	private ProcessorAnnotationProcessorContext processorAnnotationProcessorContext;

	public ProcessorIntrospectorContext(HibernateSearchMetamodelProcessorContext processorContext) {
		this.processorContext = processorContext;
	}

	public HibernateSearchMetamodelProcessorContext processorContext() {
		return processorContext;
	}

	public Elements elementUtils() {
		return processorContext().elementUtils();
	}

	public Types typeUtils() {
		return processorContext().typeUtils();
	}

	public Messager messager() {
		return processorContext().messager();
	}

	public void typeElementsByName(String typeName, TypeElement indexedEntityType) {
		this.typeName = typeName;
		this.indexedEntityType = indexedEntityType;
	}

	public TypeElement typeElementsByName(String typeName) {
		if ( Objects.equals( typeName, this.typeName ) ) {
			return indexedEntityType;
		}
		return null;
	}

	public void processorAnnotationProcessorContext(ProcessorAnnotationProcessorContext processorAnnotationProcessorContext) {
		this.processorAnnotationProcessorContext = processorAnnotationProcessorContext;
	}

	public ProcessorAnnotationProcessorContext processorAnnotationProcessorContext() {
		return processorAnnotationProcessorContext;
	}
}
