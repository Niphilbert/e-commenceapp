package com.jsp.ecommerce.controller;

import com.jsp.ecommerce.dto.Customer;
import com.jsp.ecommerce.dto.Product;
import com.jsp.ecommerce.dto.ShoppingOrder;
import com.jsp.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import com.jsp.ecommerce.service.CustomerService;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CommonController {

	@Autowired
	CustomerService customerService;
	@Autowired
	ProductService productService;

	@GetMapping("/")
	public String showHome(ModelMap map) {
		map.put("products", productService.fetchAllProducts());
		return "Home";
	}

	@GetMapping("/about-us")
	public String loadAboutUs() {
		return "AboutUs";
	}

	@GetMapping("/login")
	public String loadLogin() {
		return "Login.html";
	}

	@PostMapping("/login")
	public String login(@RequestParam String emph, @RequestParam String password, ModelMap map, HttpSession session) {
		return customerService.login(emph, password, map, session);
	}

	@GetMapping("/logout")
	public String logout(HttpSession session, ModelMap map) {
		session.invalidate();
		map.put("pass", "Logout Success");
		return "Home";
	}

}
