/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The most common interface for the field reference hierarchy.
 * @param <SR> Containing type.
 */
@Incubating
public interface FieldReference<SR> {

	String absolutePath();

	Class<SR> scopeRootType();

}
