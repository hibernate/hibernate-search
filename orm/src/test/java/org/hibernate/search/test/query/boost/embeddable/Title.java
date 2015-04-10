/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.boost.embeddable;

import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Gunnar Morling
 */
@Embeddable
public class Title {

	@Field
	private String value;

	@IndexedEmbedded(includePaths = { "value" })
	@Boost(0.7F) // rank sub-title hits a bit lower than the main title
	private SubTitle subTitle;

	Title() {
	}

	public Title(String value) {
		this.value = value;
	}

	public Title(String value, String subTitle) {
		this.value = value;
		this.subTitle = new SubTitle( subTitle );
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public SubTitle getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(SubTitle subTitle) {
		this.subTitle = subTitle;
	}

	@Override
	public String toString() {
		return "Title [value=" + value + ", subTitle=" + subTitle + "]";
	}
}
