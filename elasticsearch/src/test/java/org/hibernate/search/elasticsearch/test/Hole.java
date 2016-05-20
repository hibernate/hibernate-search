/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;

@Entity
public class Hole {

	@Id
	@GeneratedValue
	private long id;

	@Field
	private int length;

	@NumericField
	@Field
	private byte par;

	Hole() {
	}

	public Hole(int length, byte par) {
		this.length = length;
		this.par = par;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public byte getPar() {
		return par;
	}

	public void setPar(byte par) {
		this.par = par;
	}
}
