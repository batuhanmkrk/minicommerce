package com.minicommerceapi.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categories_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
})
public class Category extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
