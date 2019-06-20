/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

/**
 * A builder of markers.
 */
public interface MarkerBuilder {

	/**
	 * Build a marker.
	 *
	 * @param buildContext A object providing access to other components involved in the build process.
	 * @return A marker instance. That instance is not considered as a bean,
	 * thus is not enclosed in a {@link org.hibernate.search.engine.environment.bean.BeanHolder}.
	 * As a consequence, the marker instance <strong>must not</strong> hold references to resources
	 * that should eventually be released by Hibernate Search.
	 */
	Object build(MarkerBuildContext buildContext);

}
