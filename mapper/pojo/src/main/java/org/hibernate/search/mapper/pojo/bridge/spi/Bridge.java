/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.spi;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElement;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElementModel;

/**
 * @author Yoann Rodiere
 */
public interface Bridge<A extends Annotation> extends AutoCloseable {

	/* Solves HSEARCH-1306 */
	default void initialize(BuildContext buildContext, A parameters) {
		// Default does nothing
	}

	/*
	 * TODO add an object to define "virtual" fields: fields which are not in the index,
	 * but can be simulated on the client side when projecting and querying.
	 */
	void bind(BridgedElementModel bridgedElementModel, IndexSchemaElement indexSchemaElement);

	void toDocument(BridgedElement source, DocumentState target);

	@Override
	default void close() {
	}

}
