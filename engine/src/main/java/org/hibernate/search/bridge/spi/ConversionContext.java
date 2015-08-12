/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;


/**
 * Setting the context before invoking a conversion bridge will provide
 * more helpful exceptions in case of errors.
 * This is especially important as bridge implementations often need to
 * make assumptions about the data formats and are user provided.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public interface ConversionContext {

	/**
	 * Decorates a FieldBridge.
	 *
	 * @param delegate set the bridge that shall be used by the decoration.
	 * @return a decorated FieldBridge which should be used instead of the original delegate.
	 */
	FieldBridge oneWayConversionContext(FieldBridge delegate);

	/**
	 * Decorates a TwoWayFieldBridge.
	 *
	 * @param delegate set the bridge that shall be used by the decoration.
	 * @return a decorated TwoWayFieldBridge which should be used instead of the original delegate.
	 */
	TwoWayFieldBridge twoWayConversionContext(TwoWayFieldBridge delegate);

	/**
	 * Decorates a StringBridge.
	 *
	 * @param delegate set the bridge that shall be used by the decoration.
	 * @return a decorated StringBridge which should be used instead of the original delegate.
	 */
	StringBridge stringConversionContext(StringBridge delegate);

	/**
	 * In case the next conversion fails, the error message will point to this type.
	 *
	 * @param beanClass the class type which is going to be converted
	 * @return this for method chaining.
	 */
	ConversionContext setClass(Class<?> beanClass);

	/**
	 * In case the next conversion fails, the error message will point to the
	 * document id of the currently indexed type.
	 *
	 * @return this for method chaining.
	 */
	ConversionContext pushIdentifierProperty();

	/**
	 * The context has an internal stack for graph navigation.
	 * Pushing a property on the stack will make sure we know on which path
	 * the failure occurred.
	 * All invocations to a push need to cleanup with a {@link #popProperty()},
	 * especially after exceptions.
	 *
	 * @param property the property which is being followed for embedded indexing
	 * @return this for method chaining.
	 */
	ConversionContext pushProperty(String property);

	/**
	 * Pops the last pushed property from the stack. See {@link #pushIdentifierProperty()}
	 * and {@link #pushProperty(String)}}
	 *
	 * @return this for method chaining.
	 */
	ConversionContext popProperty();
}
