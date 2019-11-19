/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * This package contains everything related to the configuration of the Hibernate Search engine.
 *
 * <h3>Common expected types for property values</h3>
 *
 * Below are some commonly used property types across all Hibernate Search settings
 * (engine, backends, mappers, ...).
 *
 * <h4 id="bean-reference">Bean reference</h4>
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
 * <h4 id="bean-reference-multi">Multi-valued bean reference</h4>
 *
 * A multi-valued reference to a bean of type {@code T} can be passed as either:
 * <ul>
 * <li>A {@link java.util.Collection Collection} containing any
 * <a href="bean-reference">value that is accepted as a bean reference</a></li>
 * <li>or a comma-separated {@link java.lang.String String} containing any
 * <a href="bean-reference">String value that is accepted as a bean reference</a></li>
 * </ul>
 */
package org.hibernate.search.engine.cfg;