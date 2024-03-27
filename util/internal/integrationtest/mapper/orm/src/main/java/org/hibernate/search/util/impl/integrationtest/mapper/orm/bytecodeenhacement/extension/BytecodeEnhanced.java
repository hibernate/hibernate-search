/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.bytecodeenhacement.extension;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.extension.ExtendWith;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(BytecodeEnhancementExtension.class)
public @interface BytecodeEnhanced {
}
