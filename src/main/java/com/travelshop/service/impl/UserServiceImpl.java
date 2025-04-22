package com.travelshop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelshop.dto.LoginFormDTO;
import com.travelshop.dto.Result;
import com.travelshop.dto.UserDTO;
import com.travelshop.entity.User;
import com.travelshop.mapper.UserMapper;
import com.travelshop.service.IUserService;
import com.travelshop.utils.JwtUtils;
import com.travelshop.utils.RedisConstants;
import com.travelshop.utils.RegexUtils;
import com.travelshop.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.travelshop.utils.RedisConstants.USER_SIGN_KEY;
import static com.travelshop.utils.SystemConstants.USER_NICK_NAME_PREFIX;


/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
        @Resource
        private StringRedisTemplate stringRedisTemplate;

        @Resource
        private JwtUtils jwtUtils;

        @Override
        public Result sendCode(String phone, HttpSession session) {
            // 校验手机号
            if (RegexUtils.isPhoneInvalid(phone)) {
                return Result.fail("手机号格式错误");
            }
            // 生成验证码
            String code = RandomUtil.randomNumbers(6);
            // 保存验证码到Redis，设置有效期
            stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
            // 发送验证码（实际项目中这里会调用短信API）
            log.debug("发送验证码成功，验证码：{}", code);
            return Result.ok();
        }

        @Override
        public Result login(LoginFormDTO loginForm, HttpSession session) {
            // 校验手机号
            String phone = loginForm.getPhone();
            if (RegexUtils.isPhoneInvalid(phone)) {
                return Result.fail("手机号格式错误");
            }

            // 验证验证码
            String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
            String code = loginForm.getCode();
            if (cacheCode == null || !cacheCode.equals(code)) {
                return Result.fail("验证码错误");
            }

            // 查询用户，不存在则创建
            User user = lambdaQuery().eq(User::getPhone, phone).one();
            if (user == null) {
                user = createUserWithPhone(phone);
            }

            // 用户信息转DTO
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

            // 生成JWT令牌和刷新令牌
            String token = jwtUtils.generateToken(userDTO);
            String refreshToken = jwtUtils.generateRefreshToken(userDTO);

            // 创建结果对象
            Map<String, String> result = new HashMap<>();
            result.put("token", token);
            result.put("refreshToken", refreshToken);

            // 登录成功后，删除验证码
            stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + phone);

            return Result.ok(result);
        }

        @Override
        public Result logout(HttpServletRequest request) {
            // JWT是无状态的，客户端只需删除token即可
            // 如果需要服务端实现登出功能，可以将token加入黑名单
            String token = request.getHeader("authorization");
            if (StrUtil.isNotBlank(token)) {
                // 将令牌加入黑名单，设置过期时间为JWT剩余有效期
                Date expiration = jwtUtils.getExpirationDateFromToken(token);
                if (expiration != null) {
                    long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
                    if (ttl > 0) {
                        stringRedisTemplate.opsForValue().set(
                                RedisConstants.TOKEN_BLACKLIST_KEY + token,
                                "1",
                                ttl,
                                TimeUnit.SECONDS
                        );
                    }
                }
            }

            // 清除ThreadLocal
            UserHolder.removeUser();
            return Result.ok();
        }

        @Override
        public Result refreshToken(String refreshToken) {
            // 验证刷新令牌
            if (!jwtUtils.validateToken(refreshToken)) {
                return Result.fail("无效的刷新令牌");
            }

            // 从刷新令牌中获取用户ID
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            if (userId == null) {
                return Result.fail("无效的刷新令牌");
            }

            // 查询用户信息
            User user = getById(userId);
            if (user == null) {
                return Result.fail("用户不存在");
            }

            // 生成新的访问令牌
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            String newToken = jwtUtils.generateToken(userDTO);

            // 返回新令牌
            Map<String, String> result = new HashMap<>();
            result.put("token", newToken);

            return Result.ok(result);
        }

        private User createUserWithPhone(String phone) {
            User user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            user.setRole(0); // 默认为普通用户角色
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            save(user);
            return user;
        }


        @Override
    public Result sign() {
        //获取当前登陆用户
        Long id = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY +yyyyMM+ id;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //写了redis
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取当前登陆用户
        Long id = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY +yyyyMM+ id;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //获取截至本月今天的所有签到记录
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key
                , BitFieldSubCommands
                        .create()
                        .get(BitFieldSubCommands.BitFieldType
                                .unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (result==null||result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num==null||num==0){
            return Result.ok(0);
        }
        //转二进制字符串
        String binaryString = Long.toBinaryString(num);
        //计算连续签到天数
        int count=0;
        for (int i = binaryString.length()-1; i >=0; i--) {
            if (binaryString.charAt(i)=='1'){
                count++;
            }
            else {
                break;
            }
        }
        //返回
        return Result.ok(count);
    }

}
