package org.hibernate.search.test.integration.jtaspring;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Indexed;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@Table(name="container")
@DiscriminatorColumn(name = "containerType", discriminatorType = DiscriminatorType.STRING)

@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region="container")

@Indexed(index="container")
@Analyzer(impl = StandardAnalyzer.class)
public class Container extends AbstractEntity {
	@Id()
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long containerId;

	@Column(length = 255)
	private String color;
	
	/**
	 * @return the containerId
	 */
	public Long getContainerId() {
		return containerId;
	}

	/**
	 * @param containerId the containerId to set
	 */
	public void setContainerId(Long containerId) {
		this.containerId = containerId;
	}

	public String getColor() {
		return color;
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(String color) {
		this.color = color;
	}
}
