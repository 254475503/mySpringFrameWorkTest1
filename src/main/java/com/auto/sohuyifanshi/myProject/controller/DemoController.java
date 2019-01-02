package com.auto.sohuyifanshi.myProject.controller;

import com.auto.sohuyifanshi.framework.annotation.Autowired;
import com.auto.sohuyifanshi.framework.annotation.Controller;
import com.auto.sohuyifanshi.framework.annotation.RequestMapping;
import com.auto.sohuyifanshi.framework.annotation.RequestParam;
import com.auto.sohuyifanshi.myProject.service.MyDemoService;
import com.sun.deploy.net.HttpRequest;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Controller
@RequestMapping("/DemoController")
public class DemoController {
    @Autowired
    private MyDemoService myDemoService;

    @RequestMapping("/sayHello")
    public void sayHello(HttpServletRequest request, HttpServletResponse response)
    {
        try {
            response.getWriter().write("hello");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/SaySomething")
    public void saySomething(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "something") String something)
    {
        try {
            response.getWriter().write(something);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}


