/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = RoutedIndexedEntity.NAME)
@Indexed(routingBinder = @RoutingBinderRef(type = StatusRoutingBridge.Binder.class))
public class RoutedIndexedEntity {

	public enum Status {
		FIRST, SECOND, THIRD
	}

	public static final String NAME = "RoutedIndexedEntity";

	@Id
	private Integer id;

	@FullTextField
	private String text;

	private Status status;

	public RoutedIndexedEntity() {
	}

	public RoutedIndexedEntity(Integer id, String text, Status status) {
		this.id = id;
		this.text = text;
		this.status = status;
	}

	public Integer getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

}
