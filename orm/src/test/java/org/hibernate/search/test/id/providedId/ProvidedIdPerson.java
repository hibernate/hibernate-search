/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id.providedId;

import java.io.Serializable;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.LongBridge;


/**
 * @author Navin Surtani
 */
@ProvidedId(bridge = @FieldBridge(impl = LongBridge.class))
@Indexed
public class ProvidedIdPerson implements Serializable {

	// No annotations: this entity uses a ProvidedId
	private long id;

	@Field(store = Store.YES)
	private String name;
	@Field(store = Store.YES)
	private String blurb;
	@Field(analyze = Analyze.NO, store = Store.YES)
	private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBlurb() {
		return blurb;
	}

	public void setBlurb(String blurb) {
		this.blurb = blurb;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
