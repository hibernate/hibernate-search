/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.bridge.spi.FunctionBridge;

/**
 * @author Yoann Rodiere
 */
public interface FieldMappingContext<R extends FieldMappingContext<R>> {

	R name(String name);

	R bridge(String bridgeName);

	R bridge(Class<? extends FunctionBridge<?, ?>> bridgeClass);

	R bridge(String bridgeName, Class<? extends FunctionBridge<?, ?>> bridgeClass);

}
