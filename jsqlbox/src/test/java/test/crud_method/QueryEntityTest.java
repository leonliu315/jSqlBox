package test.crud_method;

import static com.github.drinkjava2.jsqlbox.SqlHelper.e;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.drinkjava2.jsqlbox.Dao;
import com.github.drinkjava2.jsqlbox.SqlHelper;

import oracle.net.aso.q;
import test.config.InitializeDatabase;
import test.crud_method.po.User;

public class QueryEntityTest {

	@Before
	public void setup() {
		InitializeDatabase.recreateTables();
		User u = new User(); 
		u.dao().execute("insert into ", u.Table(), //
				" (", u.UserName(), e("user1"), //
				", ", u.Address(), e("address1"), //
				", ", u.Age(), ")", e("1"), //
				SqlHelper.questionMarks());
		u.dao().execute("insert into ", u.Table(), //
				" (", u.UserName(), e("user2"), //
				", ", u.Address(), e("address2"), //
				", ", u.Age(), ")", e("2"), //
				SqlHelper.questionMarks());
		u.setUserName("user3");
		u.setAddress("address3");
		u.setAge(3);
		u.dao().save();
		Assert.assertEquals(3, (int) Dao.dao.queryForInteger("select count(*) from ", User.Table));
	}

	@Test
	public void queryUser() {
		// Assert.assertEquals(2, (int) Dao.dao.queryForInteger("select count(*) from ", User.Table));
		// User user = (User) Dao.dao.queryEntity(User.class, "select b.username as UNAME from users b").get(0);
		// if (user == null) {// TODO need think about next step
		// }
	}

	public static void main(String[] args) {
		InitializeDatabase.recreateTables();
		QueryEntityTest t=new QueryEntityTest();
		t.setup();
	}
}