/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common;

/**
 * An object that defines its own normalization method for testing purposes.
 * @param <S> The self type.
 */
public interface Normalizable<S> {

	S normalize();

}
