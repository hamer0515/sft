package hk.com.smartpay.sft.scheduler.impl;

import hk.com.smartpay.sft.scheduler.Scheduler;
import hk.com.smartpay.sft.scheduler.domain.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class BankScheduler implements Scheduler {

	private String bankName;

	// 调度指针
	private List<String> pc = new LinkedList<>();

	// 待调度的任务
	private Map<String, List<Log>> tasks = new HashMap<String, List<Log>>();

	// 连接消息队列
	private JmsTemplate jmsTemplate;
	private Destination destination;
	//
	// 数据库连接
	private JdbcTemplate jdbcTemplate;
	private DataSourceTransactionManager transactionManager;

	// 构造函数
	public BankScheduler(String bankName) {
		this.bankName = bankName;
	}

	// 初始化数据结构
	private void init() {
		System.out.println(bankName);
		final String pcSql = "select distinct inst_batch from log_proc where proc_stat=0 and chnl=?";
		final String dataSql = "select id, chnl, inst, inst_batch, proc_stat from log_proc where proc_stat=0 and chnl=? and inst_batch=?";
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				jdbcTemplate
						.execute("lock table log_proc in share row exclusive mode");
				List<String> batch = jdbcTemplate.queryForList(pcSql,
						String.class, bankName);
				for (String b : batch) {
					List<Log> data = jdbcTemplate.query(dataSql, new Object[] {
							bankName, b }, new RowMapper<Log>() {

						@Override
						public Log mapRow(ResultSet rs, int rowNum)
								throws SQLException {
							// TODO Auto-generated method stub
							Log l = new Log();
							l.setId(rs.getString("id"));
							l.setChnl(rs.getString("chnl"));
							l.setInst(rs.getString("inst"));
							l.setInst_batch(rs.getString("inst_batch"));
							l.setProc_stat(rs.getInt("proc_stat"));
							return l;
						}

					});

					update(b, data);
				}
				return null;
			}
		});
	}

	// 更新调度数据
	public void update(String req, List<Log> logs) {
		// lock()
		tasks.put(req, logs);
		pc.add(req);
		// unlock()
	}

	@Override
	public int schedule(int maxCount) {
		List<Log> taskArray = new ArrayList<Log>(maxCount);

		// 获取任务ID列表
		// lock();
		int taskCount = getTaskArray(maxCount, taskArray);
		// unlock();

		return dispatchTask(taskArray, taskCount);
	}

	// 获取待分发的任务ID, 最多取maxCount
	// 返回值为获取的任务条数
	private int getTaskArray(int maxCount, List<Log> taskArray) {
		// 如果tasks哈希大小为空， 说明没有任务可调
		if (tasks.size() == 0) {
			return 0;
		}

		int taskCount = 0;
		while (true) {
			// 左侧pop一个请求号
			String cur = pc.remove(0);
			// 请求号对应的流水
			List<Log> curBatch = tasks.get(cur);
			taskArray.add(curBatch.remove(0));
			taskCount++;
			// 如果对应任务为空则删除，否则添加到任务队列末尾
			if (curBatch.isEmpty()) {
				tasks.remove(cur);
			} else {
				pc.add(cur);
			}
			// 取到足够的数据，或者数据缓存为空（取不到数据）返回
			if (taskCount == maxCount || tasks.size() == 0) {
				return taskCount;
			}
		}
	}

	// 依据任务ID依次分发任务到消息队列
	private int dispatchTask(List<Log> taskArray, int taskCount) {
		// 构建SQLStatement
		final List<Object[]> parameters = new ArrayList<Object[]>();
		final String sql = "update log_proc set proc_stat=1 where id=? and proc_stat=0";
		for (Log l : taskArray) {
			parameters.add(new Object[] { l.getId() });
		}
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				jdbcTemplate.batchUpdate(sql, parameters);
				return null;
			}
		});
		// 依据记录组JMS信息
		// 发送到银行处理队列
		for (final Log l : taskArray) {
			jmsTemplate.send(destination, new MessageCreator() {
				public Message createMessage(Session session)
						throws JMSException {
					return session.createTextMessage(l.toMessage());
				}
			});
		}
		return taskArray.size();
	}

	public void showCachedData() {
		System.out.println("pc:");
		System.out.println(Arrays.toString(pc.toArray()));
		System.out.println("taskData:");
		for (String k : tasks.keySet()) {
			List<String> d = new ArrayList<String>();
			System.out.println("==key:" + k);
			for (Log l : tasks.get(k)) {
				d.add(l.toString());
			}
			System.out.println(Arrays.toString(d.toArray()));
		}
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setTransactionManager(
			DataSourceTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}
