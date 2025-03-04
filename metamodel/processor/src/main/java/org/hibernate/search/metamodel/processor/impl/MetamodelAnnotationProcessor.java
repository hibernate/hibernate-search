/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.impl;

import javax.annotation.processing.RoundEnvironment;

public interface MetamodelAnnotationProcessor {

	void process(RoundEnvironment roundEnv);

}
