package com.travelshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.dto.Result;
import com.travelshop.entity.SeckillVoucher;
import com.travelshop.entity.Voucher;
import com.travelshop.enums.BusinessType;
import com.travelshop.mapper.VoucherMapper;
import com.travelshop.service.ISeckillVoucherService;
import com.travelshop.service.IVoucherService;
import com.travelshop.utils.UnifiedCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.travelshop.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * 服务实现类
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UnifiedCache unifiedCache;


    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = unifiedCache.queryWithBloomFilter(
                "shop",
                BusinessType.CACHE_SHOP_KEY,
                shopId,
                List.class,  // 这里只能指定为List.class，无法直接指定泛型
                id -> getBaseMapper().queryVoucherOfShop(shopId),
                false,
                30L,
                TimeUnit.MINUTES
        );
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSeckillVoucher(Voucher voucher) {
        save(voucher);
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY+voucher.getId(),voucher.getStock().toString());
    }
}
