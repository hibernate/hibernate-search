/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.mapper.pojo.model.spi.ReadableProperty;

/**
 * @author Yoann Rodiere
 */
public interface IdentifierMappingCollector {

	void collect(ReadableProperty property, IdentifierBridge<?> converter);

	static IdentifierMappingCollector noOp() {
		return (p, c) -> { };
	}
}
