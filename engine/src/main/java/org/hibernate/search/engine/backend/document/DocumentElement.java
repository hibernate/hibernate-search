/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document;

/**
 * An element of a document.
 * <p>
 * Instances may represent the document root as well as a <em>partial</em> view of the document,
 * for instance a view on a specific "object" field nested inside the document.
 *
 * @author Yoann Rodiere
 */
public interface DocumentElement {

}
