package com.travelshop.controller.user;

import com.travelshop.dto.Result;
import com.travelshop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付回调控制器
 */
@Slf4j
@RestController
@RequestMapping("/payment/callback")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentService paymentService;

    /**
     * 支付宝回调
     */
    @PostMapping("/alipay")
    public String alipayCallback(@RequestParam Map<String, String> params) {
        log.info("接收支付宝回调: {}", params);

        try {
            // 1. 验证回调参数是否合法
            // 实际项目中需要验证签名等
            boolean verifyResult = verifyAlipayCallback(params);
            if (!verifyResult) {
                log.error("支付宝回调验证失败: {}", params);
                return "failure";
            }

            // 2. 解析回调参数
            String transactionId = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");

            // 3. 处理支付回调
            Integer paymentStatus;
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                paymentStatus = 3; // 支付成功
            } else {
                paymentStatus = 4; // 支付失败
            }

            Result result = paymentService.handlePaymentCallback(transactionId, paymentStatus);
            if (result.getSuccess()) {
                return "success";
            } else {
                return "failure";
            }
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return "failure";
        }
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/wxpay")
    public String wxpayCallback(@RequestBody String xmlData) {
        log.info("接收微信支付回调: {}", xmlData);

        try {
            // 1. 解析XML数据
            Map<String, String> params = parseWxPayXml(xmlData);

            // 2. 验证回调参数是否合法
            boolean verifyResult = verifyWxPayCallback(params);
            if (!verifyResult) {
                log.error("微信支付回调验证失败: {}", params);
                return getWxPayFailureResponse();
            }

            // 3. 解析回调参数
            String transactionId = params.get("out_trade_no");
            String resultCode = params.get("result_code");

            // 4. 处理支付回调
            Integer paymentStatus;
            if ("SUCCESS".equals(resultCode)) {
                paymentStatus = 3; // 支付成功
            } else {
                paymentStatus = 4; // 支付失败
            }

            Result result = paymentService.handlePaymentCallback(transactionId, paymentStatus);
            if (result.getSuccess()) {
                return getWxPaySuccessResponse();
            } else {
                return getWxPayFailureResponse();
            }
        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return getWxPayFailureResponse();
        }
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/status/{orderId}")
    public Result queryPaymentStatus(@PathVariable("orderId") Long orderId) {
        return paymentService.queryPaymentStatus(orderId);
    }

    // 以下是辅助方法，实际项目中需要根据支付渠道SDK实现

    private boolean verifyAlipayCallback(Map<String, String> params) {
        // 实际项目中需使用支付宝SDK验证签名
        return true;
    }

    private boolean verifyWxPayCallback(Map<String, String> params) {
        // 实际项目中需使用微信支付SDK验证签名
        return true;
    }

    private Map<String, String> parseWxPayXml(String xmlData) {
        // 实际项目中需使用微信支付SDK解析XML
        return new java.util.HashMap<>();
    }

    private String getWxPaySuccessResponse() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    private String getWxPayFailureResponse() {
        return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
    }
}