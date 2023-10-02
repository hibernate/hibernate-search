/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.annotations.impl.DocumentIdAnnotationProcessor;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;

/**
 * Declare a field as the document id. If set to a property, the property will be used
 * TODO: If set to a class, the class itself will be passed to the FieldBridge
 * Note that @{link org.hibernate.search.bridge.FieldBridge#get} must return the Entity id
 *
 * @author Emmanuel Bernard
 * @deprecated Use Hibernate Search 6's {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId}
 * instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Deprecated
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = DocumentIdAnnotationProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface DocumentId {
}
