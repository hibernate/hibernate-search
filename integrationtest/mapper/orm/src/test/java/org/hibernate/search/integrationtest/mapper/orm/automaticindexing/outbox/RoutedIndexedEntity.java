/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.outbox;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = RoutedIndexedEntity.NAME)
@Indexed(routingBinder = @RoutingBinderRef(type = CustomRoutingBridge.Binder.class))
public class RoutedIndexedEntity {

	public enum Color {
		Red, Blue, Green, Yellow, White
	}

	public static final String NAME = "RoutedIndexedEntity";

	@Id
	private Integer id;

	@FullTextField
	private String text;

	private Color color;

	public RoutedIndexedEntity() {
	}

	public RoutedIndexedEntity(Integer id, String text, Color color) {
		this.id = id;
		this.text = text;
		this.color = color;
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

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public String getColorName() {
		return ( color == null ) ? Color.White.name() : color.name();
	}
}
