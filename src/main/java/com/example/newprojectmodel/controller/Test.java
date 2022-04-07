package com.example.newprojectmodel.controller;

import com.example.newprojectmodel.annotation.Log;
import com.example.newprojectmodel.entity.SysUser;
import com.example.newprojectmodel.enums.BusinessType;
import org.apache.catalina.session.StandardSession;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@Controller
public class Test {

    @GetMapping("/test")
    @Log(title = "测试",businessType = BusinessType.OTHER)
    public String test(HttpServletRequest req)
    {

        return "test";

    }

    @PostMapping("/test")
    @ResponseBody
   // @Validated
    @Log(title = "登陆",businessType = BusinessType.INSERT)
    public String test1(@Valid SysUser user, BindingResult results)
    {
        if (results.hasErrors())
            return results.getFieldError().getDefaultMessage();
        return "成功返回";
    }
}
