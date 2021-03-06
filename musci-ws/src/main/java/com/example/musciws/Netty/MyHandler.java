package com.example.musciws.Netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

public class MyHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketServerHandshaker handshaker;
    private ChannelHandlerContext ctx;
    private String sessionId;
    private String table;
    private String name;


    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) { // 传统的HTTP接入
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        }
        if (msg instanceof WebSocketFrame) { // WebSocket接入
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
       ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//        super.close(ctx, promise);
        System.out.println("delete : id = " + this.sessionId + " table = " + this.table);
        //关闭连接将移除该用户消息
        InformationOperateMap.delete(this.sessionId, this.table);
        Mage mage = new Mage();
        mage.setName(this.name);
        mage.setMessage("20002");
        //将用户下线信息发送给为下线用户
        InformationOperateMap.map.get(this.table).forEach((id, iom) -> {
            try {
                iom.sead(mage);
            } catch (Exception e) {
                System.err.println(e);
            }
        });
    }

    /**
     *   处理Http请求，完成WebSocket握手
     *   注意：WebSocket连接第一次请求使用的是Http
     *
     * @param ctx
     * @param request
     */
    private void handleHttpRequest(ChannelHandlerContext ctx,FullHttpRequest request){
        // 如果HTTP解码失败，返回HTTP异常
        if (!request.getDecoderResult().isSuccess() || (!"websocket".equals(request.headers().get("Upgrade")))){
            sendHttpResponse(ctx,request,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        // 正常WebSocket的Http连接请求，构造握手响应返回
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://"+request.headers().get(HttpHeaders.Names.HOST),null,false);
        handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null){
            // 无法处理的websocket版本
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        }else{
            handshaker.handshake(ctx.channel(),request);
            this.ctx=ctx;
        }
    }

    /**
     * Http返回
     * @param ctx
     * @param request
     * @param response
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response){
        System.out.println("sendHttpResponse:7");
        // 返回应答给客户端
        if(response.getStatus().code()!=200){
            ByteBuf buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
            HttpHeaders.setContentLength(response, response.content().readableBytes());
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture cf = ctx.channel().writeAndFlush(response);
        if(!HttpHeaders.isKeepAlive(request) || response.getStatus().code() != 200){
            cf.addListeners(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 处理Socket请求
     * @param ctx
     * @param frame
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx,WebSocketFrame frame)throws Exception{
        // 判断是否是关闭链路的指令
        if (frame instanceof CloseWebSocketFrame){
            handshaker.close(ctx.channel(),(CloseWebSocketFrame)frame.retain());
            return;
        }
        // 判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 当前只支持文本消息，不支持二进制消息
        if (frame instanceof TextWebSocketFrame){
            //获取发来的消息
            String text = ((TextWebSocketFrame)frame).text();
            System.out.println("mage: "+text);
            //消息转成Mage
            Mage mage = Mage.strJson2Mage(text);
            //判断是以存在用户信息
            if(InformationOperateMap.isNo(mage)){
                //判断是否有这个聊天室
                if(InformationOperateMap.map.containsKey(mage.getTable())){
                    //判断是否有其他用户
                    if(InformationOperateMap.map.get(mage.getTable()).size() > 0){
                        InformationOperateMap.map.get(mage.getTable()).forEach((id,iom) ->{
                            try {
                                Mage mag = iom.getMage();
                                mag.setMessage("30003");
                                //发送其他用户信息给要注册用户
                                this.sendWebSocket(mag.toJson());
                            } catch (Exception e) {
                                System.err.println(e);
                            }
                        });
                    }
                }
                //添加用户
                InformationOperateMap.add(ctx, mage);
                System.out.println("add : " + mage.toJson());
            }
            //将用户发送的消息发给所有在同一聊天室内的用户
            InformationOperateMap.map.get(mage.getTable()).forEach((id, iom) -> {
                try {
                    iom.sead(mage);
                } catch (Exception e) {
                    System.err.println(e);
                }
            });
            //记录id和table 当页面刷新或浏览器关闭时，注销掉此链路
            this.sessionId = mage.getId();
            this.table = mage.getTable();
            this.name = mage.getName();
        }else {
            System.err.println("------------------error--------------------------");
        }
    }

    /**
     * WebSocket返回
     * @param msg
     * @throws Exception
     */
    public void sendWebSocket(String msg) throws Exception {
        if (this.handshaker == null || this.ctx == null || this.ctx.isRemoved()) {
            throw new Exception("尚未握手成功，无法向客户端发送WebSocket消息");
        }
        //发送消息
        this.ctx.channel().write(new TextWebSocketFrame(msg));
        this.ctx.flush();
    }
}
