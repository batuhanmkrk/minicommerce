package edu.akademik.minicommerce;

import edu.akademik.minicommerce.domain.Category;
import edu.akademik.minicommerce.domain.Product;
import edu.akademik.minicommerce.domain.User;

import java.math.BigDecimal;

public final class TestDataFactory {
    private TestDataFactory() {}

    public static User user(String name, String email) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        return u;
    }

    public static Category category(String name, String slug) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        return c;
    }

    public static Product product(String name, String sku, BigDecimal price, int stock, Category cat) {
        Product p = new Product();
        p.setName(name);
        p.setSku(sku);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategory(cat);
        return p;
    }
}
