package com.ccy.service;


import com.ccy.netty.ChatMsg;
import com.ccy.pojo.Users;
import com.ccy.pojo.vo.FriendRequestVO;
import com.ccy.pojo.vo.MyFriendsVO;

import java.util.List;

public interface UserService {

     boolean queryUsernameIsExist(String username);
     Users queryUserForLogin(String username,String pwd);
     Users saveUser(Users user);
     Users updateUsersInfo(Users users);
     Integer preconditionSearchFriends(String myUserId,String friendUsername);
     Users queryUserInfoByUsername(String username);
     void sendFriendRequest(String myUserId,String friendUserName);
     List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
     void deleteFriendRequest(String sendUserId,String acceptUserId);
     void passFriendRequest(String sendUserId,String acceptUserId);
     List<MyFriendsVO> queryMyFriends(String userId);
     String saveMsg(ChatMsg chatMsg);
     void updateMsgSigned(List<String> msgIdList);
     List<com.ccy.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);
}
