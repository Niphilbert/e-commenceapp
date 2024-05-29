package com.jsp.ecommerce.dto;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;

@Data
@Component
@Entity
public class Customer {
	@Id
	@GeneratedValue(generator = "slno")
	@SequenceGenerator(initialValue = 101, allocationSize = 1, sequenceName = "slno", name = "slno")
	private int id;
	private String name;
	private String email;
	private String password;
	private long mobile;
	private LocalDate dob;
	private String gender;
	private boolean verified;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private ShoppingCart cart;
}
