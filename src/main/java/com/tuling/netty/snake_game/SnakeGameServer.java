package com.tuling.netty.snake_game;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Websocket 聊天服务器-服务端
 */
public class SnakeGameServer {

    private int port;
    final SnakeGameEngine gameEngine;
    private final ChannelGroup channels;

    public SnakeGameServer(int port) {
        this.port = port;
        gameEngine = new SnakeGameEngine(80, 80, 500);
        channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    public void run() throws Exception {
        // 启动 游戏引擎
        gameEngine.start();
        gameEngine.setListener(new SnakeGameEngine.SnakeGameListener() {
            @Override
            public void versionChange(VersionData changeData, VersionData currentData) {
                sendVersionData(changeData);
            }
        });

        EventLoopGroup bossGroup = new NioEventLoopGroup(2); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("http-decodec", new HttpRequestDecoder());
                            pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                            pipeline.addLast("http-encodec", new HttpResponseEncoder());
                            pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                            pipeline.addLast("http-request", new HttpRequestHandler("/ws"));
                            pipeline.addLast("WebSocket-protocol", new WebSocketServerProtocolHandler("/ws"));
                            pipeline.addLast("WebSocket-request", new SnakeGameDataSynchHandler(gameEngine, channels));
                        }
                    })  //(4)
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            System.out.println("SnakeGameServer 启动了" + port);
            // 绑定端口，开始接收进来的连接
            ChannelFuture f = b.bind(port).sync(); // (7)
            // 等待服务器  socket 关闭 。
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("SnakeGameServer 关闭了");
        }
    }

    private void sendVersionData(VersionData data) {
        String str = JSON.toJSONString(data);
        for (Channel channel : channels) {
            channel.writeAndFlush(new TextWebSocketFrame(str));
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new SnakeGameServer(port).run();

    }
}