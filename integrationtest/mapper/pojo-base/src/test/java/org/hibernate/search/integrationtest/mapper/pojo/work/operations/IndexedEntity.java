/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Indexed(index = IndexedEntity.INDEX)
public final class IndexedEntity {

	public static final String INDEX = "IndexedEntity";

	public static IndexedEntity of(int id) {
		IndexedEntity entity = new IndexedEntity();
		entity.id = id;
		entity.value = String.valueOf( id );
		return entity;
	}

	@DocumentId
	Integer id;

	@GenericField
	String value;

	@IndexedEmbedded
	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "containing")))
	ContainedEntity contained;
}
