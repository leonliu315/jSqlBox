package test.crud_method;

import static com.github.drinkjava2.jsqlbox.SqlHelper.q;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.drinkjava2.BeanBox;
import com.github.drinkjava2.jsqlbox.Dao;

import test.config.Config;
import test.crud_method.po.User;

public class InsertTest {

	@Before
	public void setup() {
		Config.recreateTables();
	}

	@Test
	public void insertUser1() {
		User user = new User();
		user.setUserName("User1");
		user.setAddress("Address1");
		user.setPhoneNumber("111");
		user.setAge(10);
		user.dao().save();
		Assert.assertEquals(111,
				(int) Dao.dao.queryForInteger("select PhoneNumber from user where username=" + q("User1")));
		Assert.assertEquals((Integer) 111,
				Dao.dao.queryForInteger("select PhoneNumber from user where username=" + q("User1")));
	}

	@Test
	public void insertUser2() {
		User user2 = new User();
		user2.dao().getSqlBox().setColumnName(User.PhoneNumber, null);
		user2.setUserName("User2");
		user2.setAddress("Address2");
		user2.setPhoneNumber("222");// this phone number will not write to table
		user2.setAge(20);
		user2.dao().save();
		Assert.assertEquals(null, Dao.dao.queryForString("select PhoneNumber from user where username=" + q("User2")));
	}

	@Test
	public void tx_insertUsers() {
		insertUser1();
		insertUser2();
	}

	@Test
	public void insertUsersWithinTransaction() {
		InsertTest t = BeanBox.getBean(InsertTest.class); // get Proxy bean
		t.tx_insertUsers(); // use Spring Declarative Transaction
	}

}