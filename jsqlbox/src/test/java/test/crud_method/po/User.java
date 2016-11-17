package test.crud_method.po;

import com.github.drinkjava2.jsqlbox.Dao;

/**
 * This file should automatically created by a code generator tool
 *
 */
public class User {
	// ==== have to copy below Dao methods in each entity class:=====
	private Dao dao;

	public void putDao(Dao dao) {
		this.dao = dao;
	}

	public Dao dao() {
		if (dao == null)
			dao = Dao.defaultDao(this);
		return dao;
	}

	// ============= Dao code end =========

	public static final String Table = "users";

	public String Table() {
		return dao().getSqlBox().getTableName();
	}

	public static String Id = "id";
	private Integer id;

	private String userName;
	public static final String UserName = "userName";

	public String UserName() {
		return dao().getSqlBox().getColumn(UserName).getColumnDefinition();
	}

	public static final String PhoneNumber = "phoneNumber";
	private String phoneNumber;

	public String PhoneNumber() {
		return dao().getSqlBox().getColumn(PhoneNumber).getColumnDefinition();
	}

	public static final String Address = "address";
	private String address;

	public String Address() {
		return dao().getSqlBox().getColumn(Address).getColumnDefinition();
	}

	public static final String Age = "age";
	private Integer age;

	public String Age() {
		return dao().getSqlBox().getColumn(Age).getColumnDefinition();
	}

	public static final String Alive = "alive";
	private Boolean alive;

	public String Alive() {
		return dao().getSqlBox().getColumn(Alive).getColumnDefinition();
	}

	public Boolean getAlive() {
		return alive;
	}

	public void setAlive(Boolean alive) {
		this.alive = alive;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

}