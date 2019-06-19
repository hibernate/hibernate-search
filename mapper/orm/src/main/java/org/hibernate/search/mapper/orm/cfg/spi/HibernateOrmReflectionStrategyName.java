/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum HibernateOrmReflectionStrategyName {

	/**
	 * Use {@code java.lang.reflect} directly for reflection,
	 * such as calling a method dynamically.
	 */
	JAVA_LANG_REFLECT( "java-lang-reflect" ),

	/**
	 * Use {@code java.lang.invoke} for reflection,
	 * such as calling a method dynamically.
	 * This means using {@link java.lang.invoke.MethodHandles.Lookup} and {@link java.lang.invoke.MethodHandle}
	 * in particular.
	 */
	METHOD_HANDLE( "method-handle" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static HibernateOrmReflectionStrategyName of(String value) {
		return StringHelper.parseDiscreteValues(
				HibernateOrmReflectionStrategyName.values(),
				HibernateOrmReflectionStrategyName::getExternalRepresentation,
				log::invalidReflectionStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	HibernateOrmReflectionStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	private String getExternalRepresentation() {
		return externalRepresentation;
	}

}
