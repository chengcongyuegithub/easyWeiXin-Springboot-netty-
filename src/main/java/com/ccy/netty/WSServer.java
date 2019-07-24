package com.ccy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;


@Component
public class WSServer {

    public static class SingletionWSServer{
        static final WSServer instance=new WSServer();
    }
    public static WSServer getInstance()
    {
        return SingletionWSServer.instance;
    }
    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap serverBootstrap;
    private ChannelFuture channelFuture;

    public WSServer()
    {
        mainGroup = new NioEventLoopGroup();
        subGroup =new NioEventLoopGroup();
        serverBootstrap=new ServerBootstrap();
        serverBootstrap.group(mainGroup,subGroup).channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInitializer());
    }
    public void start()
    {
        this.channelFuture=serverBootstrap.bind(8088);
        System.err.println("netty 服务器启动!!!");
    }

    public static void main(String[] args) {
        WSServer instance = WSServer.getInstance();
        instance.start();
    }

}
