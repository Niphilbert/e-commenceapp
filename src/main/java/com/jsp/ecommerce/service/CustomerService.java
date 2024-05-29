package com.jsp.ecommerce.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jsp.ecommerce.dao.OrderDao;
import com.jsp.ecommerce.helper.AES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

import com.jsp.ecommerce.dao.CustomerDao;
import com.jsp.ecommerce.dao.ProductDao;
import com.jsp.ecommerce.dto.Customer;
import com.jsp.ecommerce.dto.Item;
import com.jsp.ecommerce.dto.PaymentDetails;
import com.jsp.ecommerce.dto.Product;
import com.jsp.ecommerce.dto.ShoppingCart;
import com.jsp.ecommerce.dto.ShoppingOrder;
import com.jsp.ecommerce.repository.PaymentDetailsRepository;
import com.jsp.ecommerce.repository.ShoppingOrderRepository;
import org.json.JSONObject;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.servlet.http.HttpSession;

@Service
public class CustomerService {

	@Autowired
	private CustomerDao customerDao;

	@Autowired
	private ProductDao productDao;

	@Autowired
	private ShoppingOrderRepository orderRepository;

	@Autowired
	private PaymentDetailsRepository detailsRepository;

	@Autowired
	private OrderDao orderDao;

	public String signup(Customer customer, ModelMap map) {
		// to check Email and Mobile is Unique
		List<Customer> exCustomers = customerDao.findByEmailOrMobile(customer.getEmail(), customer.getMobile());
		if (!exCustomers.isEmpty()) {
			map.put("fail", "Account Already Exists");
			return "Signup";
		} else {
			// Encrypting password
			customer.setPassword(AES.encrypt(customer.getPassword(), "123"));
			customerDao.save(customer);
			map.put("pass", "Account Created Successfully. You can now login.");
			return "Login";
		}
	}

	public String login(String emph, String password, ModelMap map, HttpSession session) {
		if (emph.equals("admin") && password.equals("admin")) {
			session.setAttribute("admin", "admin");
			map.put("pass", "Admin Login Success");
			return "AdminHome";
		} else {
			long mobile = 0;
			String email = null;
			try {
				mobile = Long.parseLong(emph);
			} catch (NumberFormatException e) {
				email = emph;
			}

			List<Customer> customers = customerDao.findByEmailOrMobile(email, mobile);
			if (customers.isEmpty()) {
				map.put("fail", "Invalid Email or Mobile");
				return "Login";
			} else {
				Customer customer = customers.get(0);
				if (AES.decrypt(customer.getPassword(), "123").equals(password)) {
					session.setAttribute("customer", customer);
					map.put("pass", "Login Success");
					return "redirect:/customer/home";
				} else {
					map.put("fail", "Invalid Password");
					return "Login";
				}
			}
		}
	}

	public String fetchProducts(ModelMap map, Customer customer) {
		List<Product> products = productDao.fetchAll(); // Fetch all products
		if (products.isEmpty()) {
			map.put("fail", "No Products Present");
		} else {
			map.put("products", products);
		}
		return "CustomerHome";
	}

	public String addToCart(Customer customer, int id, ModelMap map, HttpSession session) {
		Product product = productDao.findById(id);

		ShoppingCart cart = customer.getCart();
		if (cart == null) {
			cart = new ShoppingCart();
		}

		if ((cart.getTotalAmount() + product.getPrice()) < 100000) {
			List<Item> items = cart.getItems();
			if (items == null) {
				items = new ArrayList<>();
			}

			if (product.getStock() > 0) {
				boolean flag = true;
				// if item already exists in cart
				for (Item item : items) {
					if (item.getName().equals(product.getName())) {
						flag = false;
						item.setQuantity(item.getQuantity() + 1);
						item.setPrice(item.getPrice() + product.getPrice());
						break;
					}
				}
				if (flag) {
					// If item is new in cart
					Item item = new Item();
					item.setCategory(product.getCategory());
					item.setName(product.getName());
					item.setPicture(product.getPicture());
					item.setPrice(product.getPrice());
					item.setQuantity(1);
					items.add(item);
				}
				cart.setItems(items);
				cart.setTotalAmount(cart.getItems().stream().mapToDouble(Item::getPrice).sum());
				customer.setCart(cart);
				customerDao.save(customer);
				// updating stock
				product.setStock(product.getStock() - 1);
				productDao.save(product);
				session.setAttribute("customer", customer);
				map.put("pass", "Product Added to Cart");
				return fetchProducts(map, customer);
			} else {
				map.put("fail", "Out of stock");
				return fetchProducts(map, customer);
			}
		} else {
			map.put("fail", "Out of Transaction Limit");
			return fetchProducts(map, customer);
		}
	}

	public String viewCart(Customer customer, ModelMap map) {
		ShoppingCart cart = customer.getCart();
		if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
			map.put("fail", "No Items in Cart");
			return "CustomerHome";
		} else {
			map.put("cart", cart);
			return "ViewCart";
		}
	}

	public String removeFromCart(Customer customer, int id, ModelMap map, HttpSession session) {
		Product product = productDao.findById(id);

		ShoppingCart cart = customer.getCart();
		if (cart == null) {
			map.put("fail", "Item not in Cart");
			return fetchProducts(map, customer);
		} else {
			List<Item> items = cart.getItems();
			if (items == null || items.isEmpty()) {
				map.put("fail", "Item not in Cart");
				return fetchProducts(map, customer);
			} else {
				Item item = null;
				for (Item item2 : items) {
					if (item2.getName().equals(product.getName())) {
						item = item2;
						break;
					}
				}
				if (item == null) {
					map.put("fail", "Item not in Cart");
					return fetchProducts(map, customer);
				} else {
					if (item.getQuantity() > 1) {
						item.setQuantity(item.getQuantity() - 1);
						item.setPrice(item.getPrice() - product.getPrice());
					} else {
						items.remove(item);
					}
				}
				cart.setItems(items);
				cart.setTotalAmount(cart.getItems().stream().mapToDouble(Item::getPrice).sum());
				customer.setCart(cart);
				customerDao.save(customer);

				// updating stock
				product.setStock(product.getStock() + 1);
				productDao.save(product);

				if (item != null && item.getQuantity() == 1) {
					productDao.deleteItem(item);
				}

				session.setAttribute("customer", customer);
				map.put("pass", "Product Removed from Cart");
				return fetchProducts(map, customer);
			}
		}
	}

	public String createOrder(Customer customer, ModelMap map) throws RazorpayException {
		RazorpayClient client = new RazorpayClient("rzp_test_S6TGBrvbUykMqU", "Ps62zRWlFHl45Z9VXPzMN8u8");

		JSONObject object = new JSONObject();
		object.put("amount", customer.getCart().getTotalAmount() * 100);
		object.put("currency", "INR");

		Order order = client.orders.create(object);

		PaymentDetails details = new PaymentDetails();
		details.setAmount(customer.getCart().getTotalAmount());
		details.setCurrency(order.get("currency"));
		details.setDescription("Shopping Cart Payment for the products");
		details.setImage("https://www.shutterstock.com/image-vector/mobile-application-shopping-online-on-260nw-1379237159.jpg");
		details.setKeyCode("rzp_test_S6TGBrvbUykMqU");
		details.setName("Ecommerce Shopping");
		details.setOrder_id(order.get("id"));
		details.setStatus("created");

		detailsRepository.save(details);

		map.put("details", details);
		map.put("customer", customer);
		return "PaymentPage";
	}

	public String completeOrder(String payment_id, Customer customer, ModelMap map) {
		ShoppingCart cart = customer.getCart();
		if (cart != null && !cart.getItems().isEmpty()) {
			ShoppingOrder order = new ShoppingOrder();
			order.setAmount(cart.getTotalAmount());
			order.setCustomer(customer);
			order.setDateTime(LocalDateTime.now());
			order.setOrder_id(UUID.randomUUID().toString());
			order.setPayment_id(payment_id);
			order.setItems(new ArrayList<>(cart.getItems()));

			orderRepository.save(order);

			cart.getItems().clear();
			cart.setTotalAmount(0);
			customerDao.save(customer);

			map.put("pass", "Payment Complete and Order Created");
			return "redirect:/customer/fetch-orders";
		} else {
			map.put("fail", "Cart is empty, cannot complete the order.");
			return "ViewCart";
		}
	}


	public String fetchAllOrders(Customer customer, ModelMap map) {
		List<ShoppingOrder> orders = orderRepository.findByCustomer(customer);
		if (orders.isEmpty()) {
			map.put("fail", "No Orders Placed Yet");
			return "CustomerHome";
		} else {
			map.put("orders", orders);
			return "ViewOrders";
		}
	}

	public String fetchAllOrderItems(int id, Customer customer, ModelMap map) {
		ShoppingOrder order = orderRepository.findById(id).orElse(null);
		if (order != null) {
			map.put("order", order);
			return "ViewOrderItems";
		} else {
			map.put("fail", "Order Not Found");
			return "CustomerHome";
		}
	}

	public String viewOrders(Customer customer, ModelMap map) {
		List<ShoppingOrder> orders = orderDao.findByCustomer(customer);
		map.put("orders", orders);
		return "ViewOrders";
	}

	public String cancelOrder(int orderId, Customer customer, ModelMap map) {
		ShoppingOrder order = orderRepository.findById(orderId).orElse(null);
		if (order != null && order.getCustomer().getId() == customer.getId()) {
			orderRepository.delete(order);
			map.put("success", "Order canceled successfully.");
		} else {
			map.put("fail", order == null ? "Order not found." : "Order does not belong to the customer.");
		}
		return viewOrders(customer, map);
	}




	public String createOrder(Customer customer, ShoppingCart cart, ModelMap map, String paymentId) {
		ShoppingOrder order = new ShoppingOrder();
		order.setOrder_id(UUID.randomUUID().toString());
		order.setPayment_id(paymentId);
		order.setDateTime(LocalDateTime.now());
		order.setAmount(cart.getTotalAmount());
		order.setItems(cart.getItems());
		order.setCustomer(customer);

		orderDao.save(order);
		clearCart(customer); // Clear the cart after placing the order

		map.put("success", "Order created successfully.");
		return viewOrders(customer, map);
	}

	public void saveOrder(ShoppingOrder order, ShoppingCart cart) {
		orderDao.save(order);
		clearCart(cart); // Clear the cart after saving the order
	}

	public void clearCart(Customer customer) {
		ShoppingCart cart = customer.getCart();
		if (cart != null) {
			cart.getItems().clear();
			cart.setTotalAmount(0);
			customer.setCart(cart);
			customerDao.save(customer);
		}
	}

	private void clearCart(ShoppingCart cart) {
		if (cart != null) {
			cart.getItems().clear();
			cart.setTotalAmount(0);
		}
	}



}
