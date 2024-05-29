package com.jsp.ecommerce.dao;

import com.jsp.ecommerce.dto.Customer;
import com.jsp.ecommerce.dto.ShoppingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDao extends JpaRepository<ShoppingOrder, Integer> {
    List<ShoppingOrder> findByCustomer(Customer customer);
}
