package com.jsp.ecommerce.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class ShoppingOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	String order_id;
	String payment_id;
	LocalDateTime dateTime;
	double amount;

	@OneToMany
	List<Item> items;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;


	public String getDate()
	{
		return this.dateTime.format(DateTimeFormatter.ofPattern("dd-MMM-YYYY hh:mm"));
	}



}

