import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Client {

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {

        // Строка передаваемая на сервер побуквенно
        byte[] strBytes = "Welcome to Netty\n".getBytes(StandardCharsets.UTF_8);

        //Клиенту достаточно одного ThreadPool для обработки сообщений
        final NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            StringBuilder str = new StringBuilder();
                                            ByteBuf m = (ByteBuf) msg;
                                            while (m.isReadable()) {
                                                str.append((char) m.readByte());
                                            }
                                            System.out.println("Вернулась строка " + "[ " + str + " ]");
                                            ctx.close();
                                        }
                                    }
                            );
                        }
                    });

            System.out.println("Client started");

            Channel channel = bootstrap.connect("localhost", 9090).sync().channel();

            for (int i = 0; i < strBytes.length; i++) {
                ByteBuf msg = Unpooled.wrappedBuffer(strBytes, i, 1);
                channel.writeAndFlush(msg);
                Thread.sleep(1000);
            }

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}
