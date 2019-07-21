package com.ccy.controller;

import com.ccy.pojo.Users;
import com.ccy.pojo.bo.UsersBo;
import com.ccy.pojo.vo.UsersVo;
import com.ccy.service.UserSerivce;
import com.ccy.utils.CcyJSONResult;
import com.ccy.utils.FastDFSClient;
import com.ccy.utils.FileUtils;
import com.ccy.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserSerivce userSerivce;
    @Autowired
    private FastDFSClient fastDFSClient;
    @PostMapping("/registOrLogin")
    public CcyJSONResult registOrLogin(@RequestBody Users user)throws Exception
    {
        if(StringUtils.isBlank(user.getUsername())||
                StringUtils.isBlank(user.getPassword()))
        {
            return CcyJSONResult.errorMsg("用户名和密码不能为空。。。。");
        }
        boolean userNameIsExist = userSerivce.queryUsernameIsExist(user.getUsername());
        Users userResult=null;
        if(userNameIsExist)
        {
            userResult = userSerivce.queryUserForLogin(user.getUsername(), MD5Utils.getMD5Str(user.getPassword()));
            if(userResult==null)
            {
                return CcyJSONResult.errorMsg("用户名或密码不正确。。。。");
            }
        }
        else
        {
            user.setQrcode(user.getQrcode());
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userSerivce.saveUser(user);
        }
        UsersVo usersVo=new UsersVo();
        BeanUtils.copyProperties(userResult,usersVo);
        return CcyJSONResult.ok(usersVo);
    }

    @PostMapping("/uploadFaceBase64")
    public CcyJSONResult uploadFaceBase64(@RequestBody UsersBo usersBo) throws Exception
    {
         String base64Data=usersBo.getFaceData();
         //临时
         String userFacePath="D:\\"+usersBo.getUserId()+"userface64.png";
         FileUtils.base64ToFile(userFacePath,base64Data);

         MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
         String url = fastDFSClient.uploadBase64(faceFile);
         System.out.println(url);

         String thump="_80x80.";
         String[] arr=url.split("\\.");
         String thumpImgUrl=arr[0]+thump+arr[1];

         Users user=new Users();
         user.setId(usersBo.getUserId());
         user.setFaceImage(thumpImgUrl);
         user.setFaceImageBig(url);
         user=userSerivce.updateUsersInfo(user);
         return CcyJSONResult.ok(user);
    }

    @PostMapping("/setNickName")
    public CcyJSONResult setNickName(@RequestBody UsersBo usersBo) throws Exception
    {
         Users users=new Users();
         users.setId(usersBo.getUserId());
         users.setNickname(usersBo.getNickname());
         users = userSerivce.updateUsersInfo(users);

         return CcyJSONResult.ok(users);
    }
}
