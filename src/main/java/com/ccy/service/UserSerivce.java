package com.ccy.service;


import com.ccy.pojo.Users;

public interface UserSerivce {

    public boolean queryUsernameIsExist(String username);
    public Users queryUserForLogin(String username,String pwd);
    public Users saveUser(Users user);
    public Users updateUsersInfo(Users users);
}
