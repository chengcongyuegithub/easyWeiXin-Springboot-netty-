package com.ccy.service.impl;

import com.ccy.enums.MsgActionEnum;
import com.ccy.enums.MsgSignFlagEnum;
import com.ccy.enums.SearchFriendsStatusEnum;
import com.ccy.mapper.*;
import com.ccy.netty.ChatMsg;
import com.ccy.netty.DataContent;
import com.ccy.netty.UserChannelRel;
import com.ccy.pojo.FriendsRequest;
import com.ccy.pojo.MyFriends;
import com.ccy.pojo.Users;
import com.ccy.pojo.vo.FriendRequestVO;
import com.ccy.pojo.vo.MyFriendsVO;
import com.ccy.service.UserService;
import com.ccy.utils.FastDFSClient;
import com.ccy.utils.FileUtils;
import com.ccy.utils.JsonUtils;
import com.ccy.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private QRCodeUtils qrCodeUtils;
    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private UsersMapperCustom usersMapperCustom;
    @Autowired
    private Sid sid;
    @Autowired
    private FastDFSClient fastDFSClient;
    @Autowired
    private MyFriendsMapper myFriendsMapper;
    @Autowired
    private FriendsRequestMapper friendsRequestMapper;
    @Autowired
    private ChatMsgMapper chatMsgMapper;
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {
        Users users=new Users();
        users.setUsername(username);
        Users result = usersMapper.selectOne(users);
        return result!=null?true:false;
    }
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String pwd) {
        Example example=new Example(Users.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("username",username);
        criteria.andEqualTo("password",pwd);
        Users result = usersMapper.selectOneByExample(example);
        return result;
    }
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users saveUser(Users user) {
        String userId = sid.nextShort();
        String qrCodePath="D://user"+userId+"qrcode.png";
        qrCodeUtils.createQRCode(qrCodePath,"weixin_qrcode:"+user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl="";
        try {
            qrCodeUrl=fastDFSClient.uploadQRCode(qrCodeFile);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUsersInfo(Users users)
    {
         usersMapper.updateByPrimaryKeySelective(users);
         return queryUsersById(users.getId());
    }

    private Users queryUsersById(String userId)
    {
          return usersMapper.selectByPrimaryKey(userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId,String friendUsername)
    {
        Users user = queryUserInfoByUsername(friendUsername);
        if(user==null)
        {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        if(user.getId().equals(myUserId))
        {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        Example mfe=new Example(MyFriends.class);
        Example.Criteria mfc = mfe.createCriteria();
        mfc.andEqualTo("myUserId",myUserId);
        mfc.andEqualTo("myFriendUserId",user.getId());
        MyFriends myFriend = myFriendsMapper.selectOneByExample(mfe);
        if(myFriend!=null)
        {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return  SearchFriendsStatusEnum.SUCCESS.status;
    }
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users queryUserInfoByUsername(String username)
    {
        Example ue=new Example(Users.class);
        Example.Criteria criteria = ue.createCriteria();
        criteria.andEqualTo("username",username);
        return usersMapper.selectOneByExample(ue);
    }
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUserName) {
        Users friend = queryUserInfoByUsername(friendUserName);
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", myUserId);
        frc.andEqualTo("acceptUserId", friend.getId());
        FriendsRequest friendRequest = friendsRequestMapper.selectOneByExample(fre);
        if(friendRequest==null)
        {
            String requestId = sid.nextShort();
            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
          Example fre=new Example(FriendsRequest.class);
          Example.Criteria frc = fre.createCriteria();
          frc.andEqualTo("sendUserId",sendUserId);
          frc.andEqualTo("acceptUserId",acceptUserId);
          friendsRequestMapper.deleteByExample(fre);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId,acceptUserId);
        saveFriends(acceptUserId,sendUserId);
        deleteFriendRequest(sendUserId,acceptUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null) {
            // 使用websocket主动推送消息到请求发起者，更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);

            sendChannel.writeAndFlush(
                    new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContent)));
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFirends = usersMapperCustom.queryMyFriends(userId);
        return myFirends;
    }

    @Override
    public String saveMsg(ChatMsg chatMsg) {

        com.ccy.pojo.ChatMsg msgDB = new com.ccy.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    private void saveFriends(String sendUserId,String acceptUserId)
    {
         MyFriends myFriends=new MyFriends();
         String recordId = sid.nextShort();
         myFriends.setId(recordId);
         myFriends.setMyFriendUserId(acceptUserId);
         myFriends.setMyUserId(sendUserId);
         myFriendsMapper.insert(myFriends);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.ccy.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {

        Example chatExample = new Example(com.ccy.pojo.ChatMsg.class);
        Example.Criteria chatCriteria = chatExample.createCriteria();
        chatCriteria.andEqualTo("signFlag", 0);
        chatCriteria.andEqualTo("acceptUserId", acceptUserId);

        List<com.ccy.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);

        return result;
    }
}
