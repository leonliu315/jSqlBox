package test.coverage_test.id;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.drinkjava2.BeanBox;
import com.github.drinkjava2.jsqlbox.Dao;
import com.github.drinkjava2.jsqlbox.id.TableGenerator;

import test.config.PrepareTestContext;
import test.config.po.User;

public class TableGeneratorTest {

	@Before
	public void setup() {
		System.out.println("===============================Testing TableGeneratorTest===============================");
		PrepareTestContext.prepareDatasource_setDefaultSqlBoxConetxt_recreateTables();
	}

	@After
	public void cleanUp() {
		PrepareTestContext.closeDatasource_closeDefaultSqlBoxConetxt();
	}

	public static class TableGeneratorBox extends BeanBox {
		{
			this.setConstructor(TableGenerator.class, "T", "PK", "PV", "V", 1, 50);
		}
	}

	public static class TableGeneratorBox2 extends BeanBox {
		{
			this.setConstructor(TableGenerator.class, "T", "PK", "PV", "V", 1, 50);
		}
	}

	@Test
	public void insertUserInMysql() {
		if (!(Dao.getDefaultDatabaseType().isMySql() || Dao.getDefaultDatabaseType().isH2()))
			return;
		User u = new User();
		Dao.executeQuiet("drop table t");
		Dao.executeQuiet("create table t (pk varchar(5),v int(6)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
		u.box().configIdGenerator("age", BeanBox.getBean(TableGeneratorBox.class));
		u.setUserName("User1");
		for (int i = 0; i < 60; i++)
			u.insert();
		Assert.assertEquals(60, (int) Dao.queryForInteger("select count(*) from ", u.table()));

		u.box().configIdGenerator("age", BeanBox.getBean(TableGeneratorBox2.class));
		for (int i = 0; i < 60; i++)
			u.insert();
		Assert.assertEquals(120, (int) Dao.queryForInteger("select count(*) from ", u.table()));
	}

	@Test
	public void insertUserInOracle() {
		if (!Dao.getDefaultDatabaseType().isOracle())
			return;
		User u = new User();
		Dao.executeQuiet("drop table T");
		Dao.executeQuiet("CREATE TABLE T (PK VARCHAR(5),V INTEGER) ");
		u.box().configIdGenerator("age", BeanBox.getBean(TableGeneratorBox.class));
		u.setUserName("User1");
		for (int i = 0; i < 60; i++)
			u.insert();
		Assert.assertEquals(60, (int) Dao.queryForInteger("select count(*) from ", u.table()));
	}

}