package com.sparta.plate.entity;

public enum UserRoleEnum {
	CUSTOMER(Authority.CUSTOMER, null),
	OWNER(Authority.OWNER, "plate_owner"),
	MANAGER(Authority.MANAGER, "plate_manager"),
	MASTER(Authority.MASTER, "plate_master");

	private final String authority;
	private final String verificationCode;

	UserRoleEnum(String authority, String verificationCode) {
		this.authority = authority;
		this.verificationCode = verificationCode;
	}

	public String getAuthority() {
		return this.authority;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public static class Authority {
		public static final String CUSTOMER = "ROLE_CUSTOMER";
		public static final String OWNER = "ROLE_OWNER";
		public static final String MANAGER = "ROLE_MANAGER";
		public static final String MASTER = "ROLE_MASTER";
	}

	public UserRoleEnum validateVerificationCode(String inputCode) {
		if (this != CUSTOMER && !this.verificationCode.equals(inputCode)) {
			throw new IllegalArgumentException("유효하지않은 인증코드입니다.");
		}

		return this;
	}
}
