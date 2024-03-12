package org.dromara.northstar.web.restful;

import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.OrderRecall;
import org.dromara.northstar.common.model.OrderRequest;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.web.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易控制器
 * @author KevinHuangwl
 *
 */
@RestController
@RequestMapping("/northstar/trade")
public class TradeOperationController {
	
	@Autowired
	protected AccountService accountService;
	
	@PostMapping("/submit")
	public ResultBean<Boolean> submitOrder(@RequestBody OrderRequest req) throws InsufficientException {
		Assert.hasText(req.getGatewayId(), "账户网关ID不能为空");
		Assert.hasText(req.getContractId(), "合约不能为空");
		Assert.hasText(req.getPrice(), "价格不能为空");
		Assert.isTrue(req.getVolume() > 0, "下单手数必须为正整数");
		Assert.notNull(req.getTradeOpr(), "交易操作不能为空");
		return new ResultBean<>(accountService.submitOrder(req));
	}
	
	@PostMapping("/cancel")
	public ResultBean<Boolean> cancelOrder(@RequestBody OrderRecall recall) throws TradeException {
		return new ResultBean<>(accountService.cancelOrder(recall));
	}
	
}
