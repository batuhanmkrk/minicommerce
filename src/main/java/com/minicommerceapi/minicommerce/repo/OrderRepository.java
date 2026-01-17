package com.minicommerceapi.minicommerce.repo;

import com.minicommerceapi.minicommerce.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
