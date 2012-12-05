package org.flowersinthesand.portal.handler;

public class DataBean {
	
	private int number;
	private String string;

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DataBean)) {
			return false;
		}

		DataBean bean = (DataBean) obj;
		return bean.number != this.number ? 
			false : 
			bean.string == null ? 
				this.string == null : 
				bean.string.equals(this.string);
	}

}
