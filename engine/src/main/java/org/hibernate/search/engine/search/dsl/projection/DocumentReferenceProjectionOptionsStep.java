/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection;

import org.hibernate.search.engine.search.DocumentReference;

/**
 * The initial and final step in a "document reference" projection definition, where optional parameters can be set.
 */
public interface DocumentReferenceProjectionOptionsStep extends ProjectionFinalStep<DocumentReference> {

}
