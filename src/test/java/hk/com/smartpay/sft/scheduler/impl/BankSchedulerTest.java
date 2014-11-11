package hk.com.smartpay.sft.scheduler.impl;

import hk.com.smartpay.sft.scheduler.domain.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class BankSchedulerTest extends TestCase {

	private BankScheduler icbcBankScheduler;
	private JdbcTemplate jdbcTemplate;
	private DataSourceTransactionManager transactionManager;

	@Override
	public void setUp() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"applicationContext.xml");
		jdbcTemplate = (JdbcTemplate) applicationContext
				.getBean("jdbcTemplate");
		transactionManager = (DataSourceTransactionManager) applicationContext
				.getBean("transactionManager");

		// 数据初始化
		prepareData();

		icbcBankScheduler = (BankScheduler) applicationContext
				.getBean("icbcBankScheduler");
	}

	private void prepareData() {
		System.out.println("begin execute prepareData");
		final String sql = "insert into log_proc(id,chnl,inst,inst_batch,proc_stat) values(?,?,?,?,?)";
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		int batch = 3;
		int count = 5;
		final List<Object[]> parameters = new ArrayList<Object[]>();
		for (int i = 0; i < batch; i++) {
			for (int j = 0; j < count; j++) {
				parameters.add(new Object[] {
						new Integer(i * batch + j + 1).toString(), "icbc",
						"smartpay", i, 0 });
			}
		}
		System.out.println("id|chnl|inst|inst_batch|proc_stat");
		for (Object[] o : parameters) {
			System.out.println(Arrays.toString(o));
		}
		tt.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				jdbcTemplate.batchUpdate(sql, parameters);
				return null;
			}
		});
		System.out.println("end execute prepareData");
	}

	@Override
	public void tearDown() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				jdbcTemplate.execute("delete from log_proc");
				return null;
			}
		});
	}

	@Test
	public void testUpdate() {
		System.out.println();
		System.out
				.println("after init:=============================================");

		icbcBankScheduler.showCachedData();
		List<Log> d = new ArrayList<Log>();
		Log l = new Log();
		l.setId("999");
		l.setInst_batch("test");
		l.setChnl("icbc");
		l.setProc_stat(0);
		d.add(l);
		icbcBankScheduler.update("test", d);
		System.out.println();
		System.out
				.println("after update============================================");
		icbcBankScheduler.showCachedData();

		int scheduleResult = icbcBankScheduler.schedule(1);
		assertEquals(scheduleResult, 1);
		System.out.println();
		System.out
				.println("after schedule 1 element================================");
		icbcBankScheduler.showCachedData();

		scheduleResult = icbcBankScheduler.schedule(2);
		assertEquals(scheduleResult, 2);
		System.out.println();
		System.out
				.println("after schedule 2 element again==========================");
		icbcBankScheduler.showCachedData();

		scheduleResult = icbcBankScheduler.schedule(3);
		assertEquals(scheduleResult, 3);
		System.out.println();
		System.out
				.println("after schedule 3 element again==========================");
		icbcBankScheduler.showCachedData();

		scheduleResult = icbcBankScheduler.schedule(20);
		assertEquals(scheduleResult, 10);
		System.out.println();
		System.out
				.println("after schedule 20 element again but there are 10 only=====");
		icbcBankScheduler.showCachedData();

	}

}
