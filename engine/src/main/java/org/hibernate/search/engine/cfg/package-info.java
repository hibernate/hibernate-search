/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package contains everything related to the configuration of the Hibernate Search engine.
 *
 * <h2>Common expected types for property values</h2>
 *
 * Below are some commonly used property types across all Hibernate Search settings
 * (engine, backends, mappers, ...).
 *
 * <h2 id="bean-reference">Bean reference</h2>
 *
 * A reference to a bean of type {@code T} can be passed as either:
 * <ul>
 * <li>An instance of {@code T},
 * <li>or a {@link org.hibernate.search.engine.environment.bean.BeanReference BeanReference}
 * to a bean in the dependency injection context (CDI/Spring/etc.),
 * <li>or a {@link java.lang.Class Class} representing the type of such a bean,
 * <li>or a {@link java.lang.String String} representing the name of such a bean,
 * <li>or a {@link org.hibernate.search.engine.environment.bean.BeanReference BeanReference}
 * to a type exposing a public, no-arg constructor,
 * to be instantiated through that constructor outside of any injection context,
 * <li>or a {@link java.lang.Class Class} representing such type,
 * <li>or a {@link java.lang.String String} representing the fully-qualified name of such type.
 * </ul>
 * The value will be interpreted in the above order.
 * For example if CDI is used in the application, and a bean-reference property is set to a {@link java.lang.Class Class}
 * representing the type of a CDI bean which happens to expose a public, no-arg constructor,
 * then Hibernate Search will retrieve the bean through CDI, not by calling the constructor directly.
 *
 * <h2 id="bean-reference-multi">Multi-valued bean reference</h2>
 *
 * A multivalued reference to a bean of type {@code T} can be passed as either:
 * <ul>
 * <li>A {@link java.util.Collection Collection} containing any
 * <a href="bean-reference">value that is accepted as a bean reference</a></li>
 * <li>or a comma-separated {@link java.lang.String String} containing any
 * <a href="bean-reference">String value that is accepted as a bean reference</a></li>
 * <li>or any value accepted for a <a href="#bean-reference">single-valued bean reference</a></li>
 * </ul>
 */
package org.hibernate.search.engine.cfg;
