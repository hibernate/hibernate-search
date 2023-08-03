/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package acme.org.hibernate.search.integrationtest.spring.repackaged.model;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

public class MyProjection {
	public String name;

	@ProjectionConstructor
	public MyProjection(@FieldProjection(path = "name") String name) {
		this.name = name;
	}
}
