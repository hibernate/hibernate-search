/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.spi;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollector;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.model.spi.Indexable;
import org.hibernate.search.engine.mapper.model.spi.IndexableModel;

/**
 * @author Yoann Rodiere
 */
public interface Bridge<A extends Annotation> extends AutoCloseable {

	/* Solves HSEARCH-1306 */
	/*
	 * TODO accept a POJO instead of an annotation?
	 * Pro: would be cleaner and easier to use when mapping JSON for instance
	 * Con: would be more complex to implement, both on our side and on the service provider side
	 * (we'd need to map annotations to a POJO). Mapping a custom POJO to an annotation is much easier.
	 * Other con: the POJO would probably need to be mutable...
	 */
	void initialize(BuildContext buildContext, A parameters);

	void bind(IndexableModel indexableModel, IndexModelCollector indexModelCollector);

	void toDocument(Indexable source, DocumentState target);

	@Override
	default void close() {
	}

}
