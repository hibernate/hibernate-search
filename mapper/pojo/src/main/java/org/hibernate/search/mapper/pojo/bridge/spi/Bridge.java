/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.spi;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.mapper.model.spi.SearchModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoState;

/**
 * @author Yoann Rodiere
 */
public interface Bridge extends AutoCloseable {

	void contribute(IndexSchemaElement indexSchemaElement, PojoModelElement bridgedPojoModelElement,
			SearchModel searchModel);

	void write(DocumentState target, PojoState source);

	@Override
	default void close() {
	}

}
