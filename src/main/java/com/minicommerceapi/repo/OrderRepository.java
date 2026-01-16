package com.minicommerceapi.repo;

import com.minicommerceapi.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
