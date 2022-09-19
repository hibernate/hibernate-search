/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package acme.org.hibernate.search.integrationtest.spring.repackaged.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class MyEntity {

	@Id
	public Long id;

	@FullTextField(projectable = Projectable.YES)
	public String name;
}
