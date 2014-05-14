/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jtaspring;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

@Entity
@Table(name = "snert")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = "snert")

// full text search
@Indexed(index = "Snert")
@Analyzer(impl = StandardAnalyzer.class)

public class Snert {

	@Id()
	@GeneratedValue(strategy = GenerationType.AUTO)
	@DocumentId
	private Long id;

	@Column(nullable = true)
	private Date birthday;

	@Column(length = 255)
	@Field(termVector = TermVector.YES)
	private String name;

	@Column(length = 24)
	private String nickname;

	@Field(analyze = Analyze.NO)
	private Boolean cool;

	@Column(name = "readCount")
	@Field(analyze = Analyze.NO, store = Store.YES)
	private int age;

	public Snert() {
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the birthday
	 */
	public Date getBirthday() {
		return birthday;
	}

	/**
	 * @param birthday the birthday to set
	 */
	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the nickname
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * @param nickname the nickname to set
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * @return the cool
	 */
	public Boolean getCool() {
		return cool;
	}

	/**
	 * @param cool the cool to set
	 */
	public void setCool(Boolean cool) {
		this.cool = cool;
	}

	/**
	 * @return the age
	 */
	public int getAge() {
		return age;
	}

	/**
	 * @param age the age to set
	 */
	public void setAge(int age) {
		this.age = age;
	}


}
