/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;


/**
 * Partial metadata about a property, used to provide partial information
 * to service providers while building the property metadata.
 *
 * @author Yoann Rodiere
 */
public interface PartialPropertyMetadata {

	Class<?> getPropertyClass();

}
