/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.boost.embeddable;

import java.util.Locale;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.Field;

/**
 * @author Yoann Rodiere
 */
@Entity
@Boost(0.7F)
public class LocalizedTitle {

	@Id
	private Long id;

	@Field
	private Locale locale;

	@Field
	private String value;

	LocalizedTitle() {
	}

	public LocalizedTitle(Long id, Locale locale, String value) {
		this.id = id;
		this.locale = locale;
		this.value = value;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	public Locale getLocale() {
		return locale;
	}


	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "LocalizedTitle [value=" + value + ", locale=" + locale + "]";
	}
}
