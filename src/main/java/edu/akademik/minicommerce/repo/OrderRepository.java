package edu.akademik.minicommerce.repo;

import edu.akademik.minicommerce.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
