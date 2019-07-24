package com.ccy.mapper;

import com.ccy.pojo.Users;
import com.ccy.pojo.vo.FriendRequestVO;
import com.ccy.pojo.vo.MyFriendsVO;
import com.ccy.utils.MyMapper;

import java.util.List;

public interface UsersMapperCustom extends MyMapper<Users> {

    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
    public List<MyFriendsVO> queryMyFriends(String userId);
    public void batchUpdateMsgSigned(List<String> msgIdList);
}
