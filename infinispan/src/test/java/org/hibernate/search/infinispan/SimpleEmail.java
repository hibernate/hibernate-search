/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Sanne Grinovero
 */
@Entity
@Indexed(index = "emails")
public class SimpleEmail {

	@Id
	@GeneratedValue
	public Long id;

	@Field(analyze = Analyze.NO)
	@Column(name = "recipient")
	public String to = "";

	@Field(store = Store.COMPRESS)
	public String message = "";

	@Field(analyze = Analyze.NO)
	public Integer sequential;

}
