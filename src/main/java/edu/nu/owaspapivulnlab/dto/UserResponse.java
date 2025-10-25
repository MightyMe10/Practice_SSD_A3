package edu.nu.owaspapivulnlab.dto;

public class UserResponse {
	private final Long id;
	private final String username;
	private final String email;
	private final String role;
	private final Boolean isAdmin;

	public UserResponse(Long id, String username, String email) {
		this(id, username, email, null, null);
	}

	public UserResponse(Long id, String username, String email, String role, Boolean isAdmin) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.role = role;
		this.isAdmin = isAdmin;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public Boolean getIsAdmin() {
		return isAdmin;
	}
}
