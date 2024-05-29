package com.jsp.ecommerce.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jsp.ecommerce.dao.ProductDao;
import com.jsp.ecommerce.dto.Product;

@Service
public class ProductService {

    @Autowired
    private ProductDao productDao;

    public List<Product> fetchAllProducts() {
        return productDao.fetchAll();
    }

    public List<Product> searchProducts(String query) {
        return productDao.findByNameContainingIgnoreCase(query);
    }
}
