/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.outboxtable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = RoutedIndexedEntity.INDEX_NAME)
public class RoutedIndexedEntity {

	public enum Color {
		Red, Blue, Green, Yellow, White
	}

	public static final String INDEX_NAME = "RoutedIndexedEntity";

	@Id
	private Integer id;

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
