/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

public interface StandardIndexSchemaFieldTypedContext<S extends StandardIndexSchemaFieldTypedContext<? extends S, F>, F>
		extends IndexSchemaFieldTypedContext<S, F> {

	S projectable(Projectable projectable);

	S sortable(Sortable sortable);

}
