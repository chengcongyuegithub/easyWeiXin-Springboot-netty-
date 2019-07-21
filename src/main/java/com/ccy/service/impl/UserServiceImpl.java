package com.ccy.service.impl;

import com.ccy.mapper.UsersMapper;
import com.ccy.pojo.Users;
import com.ccy.service.UserSerivce;
import com.ccy.utils.FastDFSClient;
import com.ccy.utils.FileUtils;
import com.ccy.utils.QRCodeUtils;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

@Service
public class UserServiceImpl implements UserSerivce {

    @Autowired
    private QRCodeUtils qrCodeUtils;
    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private Sid sid;
    @Autowired
    private FastDFSClient fastDFSClient;
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
        qrCodeUtils.createQRCode(qrCodePath,"weixin_qrcode"+user.getUsername());
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


}
