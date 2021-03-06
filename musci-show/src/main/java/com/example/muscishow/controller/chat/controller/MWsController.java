package com.example.muscishow.controller.chat.controller;

import com.example.muscishow.until.radom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class MWsController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/show/chat")
    public String webSocket(Model model){
        try{
            model.addAttribute("username", radom.randomName());
            logger.info("跳转到mywebsocket的页面上");
            return "webTable.html";
        }
        catch (Exception e){
            logger.info("跳转到websocket的页面上发生异常，异常信息是："+e.getMessage());
            return "error";
        }
    }
}
