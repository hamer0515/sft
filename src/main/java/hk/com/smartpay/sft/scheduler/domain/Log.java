package hk.com.smartpay.sft.scheduler.domain;

public class Log {
	public String id;
	public String inst;
	public String inst_batch;
	public String chnl;
	public String data;
	public int proc_stat;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getInst() {
		return inst;
	}

	public void setInst(String inst) {
		this.inst = inst;
	}

	public String getInst_batch() {
		return inst_batch;
	}

	public void setInst_batch(String inst_batch) {
		this.inst_batch = inst_batch;
	}

	public String getChnl() {
		return chnl;
	}

	public void setChnl(String chnl) {
		this.chnl = chnl;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public int getProc_stat() {
		return proc_stat;
	}

	public void setProc_stat(int proc_stat) {
		this.proc_stat = proc_stat;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return id + "|" + chnl + "|" + inst + "|" + inst_batch + "|" + data
				+ "|" + proc_stat;
	}

	public String toMessage() {
		return this.toString();
	}

}
