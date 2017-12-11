/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.spi;

import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.pojo.model.spi.Indexable;
import org.hibernate.search.mapper.pojo.model.spi.IndexableModel;

/**
 * @author Yoann Rodiere
 */
public interface RoutingKeyBridge extends AutoCloseable {

	default void initialize(BuildContext buildContext) {
	}

	void bind(IndexableModel indexableModel);

	String toRoutingKey(String tenantIdentifier, Object entityIdentifier, Indexable source);

	@Override
	default void close() {
	}

}
