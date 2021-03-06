package com.example.musicapi.show.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.musicapi.common.ResultUtils.Result;
import com.example.musicapi.common.ResultUtils.ResultFactory;
import com.example.musicapi.common.dto.CommonContextDto;
import com.example.musicapi.common.model.User;
import com.example.musicapi.common.service.impl.SecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;

/**
 * show登入登出控制器
 * wjk
 */
@RestController
@RequestMapping(value = "/show")
public class SLoginController {

    @Autowired
    private SecurityServiceImpl securityService;

    private final static Logger log = LoggerFactory.getLogger(SLoginController.class);

    /**
     *  show登入
     * @param user
     * @param
     * @return
     */
    @PostMapping(value = "/login")
    public Object vueLogin(@RequestBody User user, HttpServletRequest request){
        log.info("\n-------------------Method : login--------------------\n");
        try{
            //账号密码认证成功之后创建用户令牌时间为20分钟。并存入redis里
            CommonContextDto commonContext = securityService.createUserContext(user.getAccount(),user.getPassword(),request);
            JSONObject object = new JSONObject();
            object.put("commonContext",commonContext);
            //          createUserContext里已经存了taken了
//          redisTemplate.opsForValue().set(taken,user.getAccount(),6, TimeUnit.HOURS);//以token为key，用户账号为值设置6小时过期时间
            if (commonContext.getActive().equals("N")){
                return ResultFactory.buildFailResult("你被禁止登录");
            }
            if (commonContext.getActive().equals("D")){
                return ResultFactory.buildFailResult("此账号已被删除，请联系管理员恢复");
            }
            Result result= ResultFactory.buildSuccessResult(object);
            return result;
        }catch (Exception e){
            log.debug(e.getMessage());
            Result result=ResultFactory.buildFailResult(e.getMessage());
            return result;
        }
    }

    /**
     * 登出
     * @param
     * @return
     */
    @RequestMapping(value = "/logout")
    public Result loginOut(HttpServletRequest request){
        log.info("\n-------------------Method : login--------------------\n");
        String token = request.getHeader("X-Token");
        try {
            if (token == null) {
                Cookie[] cookies = request.getCookies();
                for (Cookie cookie : cookies) {
                    switch (cookie.getName()) {
                        case "token":
                            token = URLDecoder.decode(cookie.getValue(), "utf-8");
                            break;
                        default:
                            break;
                    }
                }
            }
        }catch (Exception e){
            log.error("登出出错：" + e);
        }
        securityService.deleteToken(token);
        return ResultFactory.buidResult(200,"您已登出","");
    }
}
