package org.hibernate.search.test.embedded;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

/**
 * Some product type
 *
 * @author Samppa Saarela
 */
@Entity
@Indexed(index = "AbstractProduct") // Indexed in common index
public class Book extends AbstractProduct {
}
