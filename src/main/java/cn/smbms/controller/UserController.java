package cn.smbms.controller;

import cn.smbms.pojo.Role;
import cn.smbms.pojo.User;
import cn.smbms.service.role.RoleService;
import cn.smbms.service.role.RoleServiceImpl;
import cn.smbms.service.user.UserService;
import cn.smbms.service.user.UserServiceImpl;
import cn.smbms.tools.Constants;
import cn.smbms.tools.PageSupport;
import com.alibaba.fastjson.JSONArray;
import com.mysql.jdbc.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.InternalResourceView;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RoleService roleService;

    //1.用户发出请求，进入登录页面
    @RequestMapping(path = {"/login","/login.html","login.jsp"})
    public String login(){
        return "login";
    }

    //2.用户填写登录信息，请求进行登录处理
    @RequestMapping(value = "doLogin",method = RequestMethod.POST)
    public String doLogin(Model model, String userCode, String userPassword, HttpServletRequest request){
        System.out.println("login ============ " );
        //获取用户名和密码
       // String userCode = request.getParameter("userCode");
       // String userPassword = request.getParameter("userPassword");
        //调用service方法，进行用户匹配
       // UserService userService = new UserServiceImpl();
        User user = userService.login(userCode,userPassword);
        if(null != user){//登录成功
            //放入session
           // request.getSession().setAttribute(Constants.USER_SESSION, user);
            request.getSession().setAttribute(Constants.USER_SESSION, user);
            //页面跳转（frame.jsp）
            //response.sendRedirect("jsp/frame.jsp");
            //跳转到frame处理器
            return "redirect:/frame";
        }else{
            //页面跳转（login.jsp）带出提示信息--转发
           // request.setAttribute("error", "用户名或密码不正确");
            //request.getRequestDispatcher("login.jsp").forward(request, response);
            //model.addAttribute("error", "用户名或密码不正确");
            request.setAttribute("error", "用户名或密码不正确");
            return "login";
        }
    }

    //3.用户登录成功，跳转主页
    @RequestMapping("/frame")
    public String main(HttpSession session){
        if(session.getAttribute(Constants.USER_SESSION)==null){
            System.out.println("用户未登录！");
            return "login";
        }else{
            return "frame";
        }
    }

    //4.异常处理
    @RequestMapping(value = "/exception",method = RequestMethod.GET)
    public String exLogin(@RequestParam String userCode,@RequestParam String userPassword){
        //调用service方法，进行用户匹配
        System.out.println("exLogin--------");
        User user=userService.login(userCode,userPassword);
        if(null==user){
            throw new RuntimeException("用户名或密码不正确--全局");
        }
            return "redirect:/frame";
    }
     /*   //局部异常处理方法
        @ExceptionHandler(value = {RuntimeException.class})
        public String handlerException(RuntimeException e,HttpServletRequest req){
        req.setAttribute("e",e);
        return "error";
        }*/

     //5.登出
     @RequestMapping("/loginout")
     public String loginOut(HttpSession session){
            //清楚session
            session.removeAttribute(Constants.USER_SESSION);
            return "login";
        }

     //6.查询用户列表信息
    @RequestMapping(value = "/userlist")
    public String getUserList(Model model,@RequestParam(value = "queryname",required = false)String queryUserName,
                              @RequestParam(value = "queryUserRole",required = false)String queryUserRole,
                              @RequestParam(value = "pageIndex",required = false)String pageIndex) {
        int _queryUserRole = 0;
        List<User> userList = null;
        //设置页面容量
        int pageSize = Constants.pageSize;
        //设置当前页码
        int currentPageNo = 1;
        if (queryUserName == null) {
            queryUserName = "";
        }
        if (queryUserRole != null && !queryUserRole.equals("")) {
            _queryUserRole = Integer.parseInt(queryUserRole);
        }
        if (pageIndex != null) {
            try {
                currentPageNo = Integer.valueOf(pageIndex);
            } catch (NumberFormatException e) {
                return "redirect:/error";
            }
        }
        //总数量（表）
        int totalCount	= userService.getUserCount(queryUserName,_queryUserRole);
        //总页数
        PageSupport pages=new PageSupport();
        pages.setCurrentPageNo(currentPageNo);
        pages.setPageSize(pageSize);
        pages.setTotalCount(totalCount);

        int totalPageCount = pages.getTotalPageCount();

        //控制首页和尾页
        if(currentPageNo < 1){
            currentPageNo = 1;
        }else if(currentPageNo > totalPageCount){
            currentPageNo = totalPageCount;
        }

        userList = userService.getUserList(queryUserName,_queryUserRole,currentPageNo, pageSize);
        model.addAttribute("userList", userList);
        List<Role> roleList = null;
        roleList = roleService.getRoleList();
        model.addAttribute("roleList",roleList);
        model.addAttribute("queryUserName",queryUserName);
        model.addAttribute("queryUserRole",queryUserRole);
        model.addAttribute("totalPageCount",totalPageCount);
        model.addAttribute("totalCount",totalCount);
        model.addAttribute("currentPageNo",currentPageNo);
        return "userlist";
    }

    //7.获取用户角色
    @ResponseBody
    @RequestMapping(value = "/rolelist",produces = "application/json;charset=utf-8")
    public String getRoleList(){
        List<Role> roleList = null;
        roleList = roleService.getRoleList();
        return JSONArray.toJSONString(roleList);
    }

    //8.修改用户密码
    @RequestMapping(value = "/pwdmodify")
    public String pwdModify(){
            return "pwdmodify";
    }
    @RequestMapping(value = "/updatepwd",method = RequestMethod.POST)
    public String updatePwd(HttpServletRequest request, String newpassword) {
        Object o = request.getSession().getAttribute(Constants.USER_SESSION);
        boolean flag = false;
        if(o != null && !StringUtils.isNullOrEmpty(newpassword)){
            flag = userService.updatePwd(((User)o).getId(),newpassword);
            if(flag){
                request.setAttribute(Constants.SYS_MESSAGE, "修改密码成功,请退出并使用新密码重新登录！");
                request.getSession().removeAttribute(Constants.USER_SESSION);//session注销
            }else{
                request.setAttribute(Constants.SYS_MESSAGE, "修改密码失败！");
            }
        }else{
            request.setAttribute(Constants.SYS_MESSAGE, "修改密码失败！");
        }
        return "pwdmodify";
    }

    @ResponseBody
    @RequestMapping(value = "/oldPwdQuery",produces = "application/json;charset=utf-8")
    public String getPwdByUserId(HttpServletRequest request, String oldpassword) {
        Object o = request.getSession().getAttribute(Constants.USER_SESSION);
        Map<String, String> resultMap = new HashMap<String, String>();

        if (null == o) {//session过期
            resultMap.put("result", "sessionerror");
        } else if (StringUtils.isNullOrEmpty(oldpassword)) {//旧密码输入为空
            resultMap.put("result", "error");
        } else {
            String sessionPwd = ((User) o).getUserPassword();
            if (oldpassword.equals(sessionPwd)) {
                resultMap.put("result", "true");
            } else {//旧密码输入不正确
                resultMap.put("result", "false");
            }
        }
                    return JSONArray.toJSONString(resultMap);
    }
     @RequestMapping("/view")
        public String getUserById(HttpServletRequest request, String id){
            if(!StringUtils.isNullOrEmpty(id)){
             //调用后台方法得到user对象
             User user = userService.getUserById(id);
            request.setAttribute("user", user);
             // request.getRequestDispatcher(url).forward(request, response);
             return "userview";
       }
              return "userlist";
   }

   //9.用户信息修改用户
    @RequestMapping("/modify")
    public String modifyrById(HttpServletRequest request, String id){
        if(!StringUtils.isNullOrEmpty(id)){
            //调用后台方法得到user对象
            User user = userService.getUserById(id);
            request.setAttribute("user", user);
            // request.getRequestDispatcher(url).forward(request, response);
            return "usermodify";
        }
        return "userlist";
    }

    //用户信息修改后保存
    @RequestMapping(value = "/modifyUserInfo", method = RequestMethod.POST)
    public String modifysave(User user) {
        user.setModifyDate(new Date());
        boolean flag=userService.modify(user);
        System.out.println("flag"+flag);
        if(userService.modify(user)){
            System.out.println("修改成功");
            return "redirect:userlist";

        }else{
            System.out.println("修改失败");
            return "usermodify";
        }
    }

    //10.删除用户
    @ResponseBody
    @RequestMapping("/deluser")
    public String  delUser(HttpServletRequest request, String uid){
        Integer delId = 0;
        try{
            delId = Integer.parseInt(uid);
        }catch (Exception e) {
            delId = 0; }
        HashMap<String, String> resultMap = new HashMap<String, String>();
        if(delId <= 0){
            resultMap.put("delResult", "notexist");
        }else{
            if(userService.deleteUserById(delId)){
                resultMap.put("delResult", "true");
            }else{
                resultMap.put("delResult", "false");
            }
        }
        return JSONArray.toJSONString(resultMap);
    }

    //11.新增用户列表信息
    @RequestMapping(value ="/useradd")
    public String useradd(@ModelAttribute("user")User user){
        return "useradd";
    }
    @RequestMapping(value = "/adduser",method = RequestMethod.POST)
    public String add(HttpServletRequest request,User user){
        user.setCreationDate(new Date());
        user.setCreatedBy(((User)request.getSession().getAttribute(Constants.USER_SESSION)).getId());
        if(userService.add(user)){
            return "redirect:/userlist";
        }
            return "useradd";
    }

    //判断账号是否存在
    @ResponseBody
    @RequestMapping(value = "ucexist",produces = "application/json;charset=utf-8")
    public String  userCodeExist(String userCode){
        HashMap<String, String> resultMap = new HashMap<String, String>();
        if(StringUtils.isNullOrEmpty(userCode)){
            resultMap.put("userCode", "exist");
        }else{
            User user = userService.selectUserCodeExist(userCode);
            if(null != user){
                resultMap.put("userCode","exist");
            }else{
                resultMap.put("userCode", "notexist");
            }
        }
        return JSONArray.toJSONString(resultMap);
    }

}