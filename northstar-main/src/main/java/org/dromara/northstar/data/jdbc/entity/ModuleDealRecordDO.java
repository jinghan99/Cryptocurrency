package org.dromara.northstar.data.jdbc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.dromara.northstar.common.model.ModuleDealRecord;

import com.alibaba.fastjson2.JSON;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name="MODULE_DEAL", indexes = {
		@Index(name="idx_deal_moduleName", columnList = "moduleName"),
})
public class ModuleDealRecordDO {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private int id;
	
	private String moduleName;
	
	private long createTime;
	
	@Lob
	private String dataStr;
	
	public ModuleDealRecordDO(String moduleName, String dataStr) {
		this.moduleName = moduleName;
		this.createTime = System.currentTimeMillis();
		this.dataStr = dataStr;
	}
	
	public static ModuleDealRecordDO convertFrom(ModuleDealRecord dealRecord) {
		return new ModuleDealRecordDO(dealRecord.getModuleName(), JSON.toJSONString(dealRecord));
	}

	public ModuleDealRecord convertTo() {
		return JSON.parseObject(dataStr, ModuleDealRecord.class);
	}
	
}
