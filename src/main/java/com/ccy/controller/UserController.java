package com.ccy.controller;

import com.ccy.enums.OperatorFriendRequestTypeEnum;
import com.ccy.enums.SearchFriendsStatusEnum;
import com.ccy.pojo.Users;
import com.ccy.pojo.bo.UsersBo;
import com.ccy.pojo.vo.MyFriendsVO;
import com.ccy.pojo.vo.UsersVo;
import com.ccy.service.UserService;
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

import java.util.List;

@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserService userService;
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
        boolean userNameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult=null;
        if(userNameIsExist)
        {
            userResult = userService.queryUserForLogin(user.getUsername(), MD5Utils.getMD5Str(user.getPassword()));
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
            userResult = userService.saveUser(user);
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
         user= userService.updateUsersInfo(user);
         return CcyJSONResult.ok(user);
    }

    @PostMapping("/setNickName")
    public CcyJSONResult setNickName(@RequestBody UsersBo usersBo) throws Exception
    {
         Users users=new Users();
         users.setId(usersBo.getUserId());
         users.setNickname(usersBo.getNickname());
         users = userService.updateUsersInfo(users);

         return CcyJSONResult.ok(users);
    }
    @PostMapping("/search")
    public CcyJSONResult searchUser(String myUserId,String friendUsername) throws Exception
    {
        if(StringUtils.isBlank(myUserId)||StringUtils.isBlank(friendUsername))
        {
            return CcyJSONResult.errorMsg("查询内容不能为空");
        }
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if(status==SearchFriendsStatusEnum.SUCCESS.status)
        {
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVo usersVo=new UsersVo();
            BeanUtils.copyProperties(user,usersVo);
            return CcyJSONResult.ok(usersVo);
        }else
        {
            String msgError = SearchFriendsStatusEnum.getMsgByKey(status);
            return CcyJSONResult.errorMsg(msgError);
        }
    }
    @PostMapping("/addFriendRequest")
    public CcyJSONResult addFriendRequest(String myUserId,String friendUserName)throws Exception
    {
         if(StringUtils.isBlank(myUserId)||StringUtils.isBlank(friendUserName))
         {
             return CcyJSONResult.errorMsg("");
         }
        Integer status = userService.preconditionSearchFriends(myUserId, friendUserName);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUserName);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return CcyJSONResult.errorMsg(errorMsg);
        }
        return CcyJSONResult.ok();
    }

    @PostMapping("/queryFriendRequests")
    public CcyJSONResult queryFriendRequests(String userId)
    {
        if (StringUtils.isBlank(userId)) {
            return CcyJSONResult.errorMsg("");
        }
        return CcyJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    @PostMapping("/operFriendRequest")
    public CcyJSONResult operFriendRequest(String acceptUserId, String sendUserId,
                                             Integer operType)
    {
        if (StringUtils.isBlank(acceptUserId)
                || StringUtils.isBlank(sendUserId)
                || operType == null) {
            return CcyJSONResult.errorMsg("");
        }
        //枚举值0或者是1
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return CcyJSONResult.errorMsg("");
        }
        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        }else if(operType == OperatorFriendRequestTypeEnum.PASS.type)
        {
            userService.passFriendRequest(sendUserId,acceptUserId);
        }

        return CcyJSONResult.ok();
    }

    @PostMapping("/myFriends")
    public CcyJSONResult myFriends(String userId) {
        // 0. userId 判断不能为空
        if (StringUtils.isBlank(userId)) {
            return CcyJSONResult.errorMsg("");
        }

        // 1. 数据库查询好友列表
        List<MyFriendsVO> myFirends = userService.queryMyFriends(userId);

        return CcyJSONResult.ok(myFirends);
    }

    @PostMapping("/getUnReadMsgList")
    public CcyJSONResult getUnReadMsgList(String acceptUserId) {
        // 0. userId 判断不能为空
        if (StringUtils.isBlank(acceptUserId)) {
            return CcyJSONResult.errorMsg("");
        }

        // 查询列表
        List<com.ccy.pojo.ChatMsg> unreadMsgList = userService.getUnReadMsgList(acceptUserId);

        return CcyJSONResult.ok(unreadMsgList);
    }
}
