package com.jsp.ecommerce.controller;

import com.jsp.ecommerce.dao.ProductDao;
import com.jsp.ecommerce.dto.Customer;
import com.jsp.ecommerce.dto.Product;
import com.jsp.ecommerce.dto.ShoppingCart;
import com.jsp.ecommerce.dto.ShoppingOrder;
import com.jsp.ecommerce.service.CustomerService;
import com.jsp.ecommerce.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/customer")
@Controller
public class CustomerController {

	@Autowired
	Customer customer;

	@Autowired
	CustomerService customerService;
	@Autowired
	ProductService productService;

	@Autowired
	ProductDao productDao;

	@GetMapping("/signup")
	public String loadSignup(ModelMap map) {
		map.put("customer", customer);
		return "Signup";
	}

	@GetMapping("/home")
	public String loadHome(HttpSession session, ModelMap map) {
		if (session.getAttribute("customer") != null) {
			customerService.fetchProducts(map, (Customer) session.getAttribute("customer"));
			return "CustomerHome";
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@PostMapping("/signup")
	public String signup(@Valid Customer customer, BindingResult result, ModelMap map) {
		if (result.hasErrors()) {
			return "Signup";
		} else {
			return customerService.signup(customer, map);
		}
	}

	public String fetchProducts(ModelMap map, Customer customer) {
		List<Product> products = productDao.fetchDisplayProducts();
		if (products.isEmpty()) {
			map.put("fail", "No Products Present");
		} else {
			map.put("products", products);
		}
		return "CustomerHome";
	}

	@GetMapping("/cart-add/{id}")
	public String addToCart(HttpSession session, ModelMap map, @PathVariable int id) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			return customerService.addToCart(customer, id, map, session);
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@GetMapping("/fetch-cart")
	public String viewCart(HttpSession session, ModelMap map) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			return customerService.viewCart(customer, map);
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@GetMapping("/cart-remove/{id}")
	public String removeFromCart(HttpSession session, ModelMap map, @PathVariable int id) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			return customerService.removeFromCart(customer, id, map, session);
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@GetMapping("/payment")
	public String createOrder(HttpSession session, ModelMap map) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			map.put("success", "Order created successfully. Proceed to payment.");
			return "CustomerCart";
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@PostMapping("/payment-complete")
	public String completeOrder(HttpSession session, ModelMap map, @RequestParam String card_number,
								@RequestParam String card_holder, @RequestParam String expiry_date,
								@RequestParam String cvv, @RequestParam String payment_id, @RequestParam String order_id) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			return customerService.completeOrder(payment_id, customer, map);
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@GetMapping("/fetch-orders")
	public String fetchOrders(HttpSession session, ModelMap map) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			return customerService.viewOrders(customer, map);
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}


	@GetMapping("/cancel-order/{id}")
	public String cancelOrder(@PathVariable int id, HttpSession session, ModelMap map) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			return customerService.cancelOrder(id, customer, map);
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@PostMapping("/complete-payment")
	@ResponseBody
	public Map<String, Object> completePayment(@RequestBody Map<String, Object> paymentData, HttpSession session) {
		Map<String, Object> response = new HashMap<>();
		Customer customer = (Customer) session.getAttribute("customer");
		ShoppingCart cart = (ShoppingCart) session.getAttribute("cart");

		if (customer != null && cart != null) {
			try {
				String orderId = (String) paymentData.get("orderId");
				String paymentId = (String) paymentData.get("paymentId");
				double amount = (Double) paymentData.get("amount");

				ShoppingOrder order = new ShoppingOrder();
				order.setOrder_id(orderId);
				order.setPayment_id(paymentId);
				order.setDateTime(LocalDateTime.now());
				order.setAmount(amount);
				order.setCustomer(customer);
				order.setItems(new ArrayList<>(cart.getItems()));

				customerService.saveOrder(order, cart);

				response.put("success", true);
			} catch (Exception e) {
				response.put("success", false);
				response.put("message", e.getMessage());
			}
		} else {
			response.put("success", false);
			response.put("message", "Session expired or cart is empty.");
		}

		return response;
	}

	@PostMapping("/clear-cart")
	public String clearCart(HttpSession session, ModelMap map) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (customer != null) {
			customerService.clearCart(customer);
			return "redirect:/customer/fetch-cart";
		} else {
			map.put("fail", "Session Expired, Login Again");
			return "Home";
		}
	}

	@GetMapping("/search")
	public String search(@RequestParam("query") String query, Model model) {
		List<Product> products = productService.searchProducts(query);
		model.addAttribute("products", products);
		return "CustomerHome";
	}
}
