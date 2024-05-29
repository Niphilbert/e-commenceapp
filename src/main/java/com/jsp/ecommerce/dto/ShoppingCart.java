package com.jsp.ecommerce.dto;

import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class ShoppingCart {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	List<Item> items = new ArrayList<>();

	double totalAmount;

	public void clear() {
		this.items.clear();
		this.totalAmount = 0.0;
	}

	public double getTotalAmount() {
		return items.stream().mapToDouble(Item::getPrice).sum();
	}
}
