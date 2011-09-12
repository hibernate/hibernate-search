package org.hibernate.search.test.configuration.norms;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed(index = "test")
public class Test {
	@Id
	@GeneratedValue
	private int id;

	@Field
	private String withNormsImplicit;

	@Field(norms = Norms.YES)
	private String withNormsExplicit;

	@Field(norms = Norms.NO)
	private String withoutNorms;

	public String getWithNormsImplicit() {
		return withNormsImplicit;
	}

	public void setWithNormsImplicit(String withNormsImplicit) {
		this.withNormsImplicit = withNormsImplicit;
	}

	public String getWithNormsExplicit() {
		return withNormsExplicit;
	}

	public void setWithNormsExplicit(String withNormsExplicit) {
		this.withNormsExplicit = withNormsExplicit;
	}

	public String getWithoutNorms() {
		return withoutNorms;
	}

	public void setWithoutNorms(String withoutNorms) {
		this.withoutNorms = withoutNorms;
	}
}


