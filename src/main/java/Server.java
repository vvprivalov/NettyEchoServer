import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;

public class Server {
    private final int port;

    public static void main(String[] args) throws InterruptedException {
        new Server(9090).start();
    }

    public Server(int port) {

        this.port = port;
    }

    public void start() throws InterruptedException {
        //ThreadPool отвечающий за инициализацию новых подключений
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //ThreadPool обслуживающий всех активных клиентов
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) //Используем серверную версию сокета
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {

                            // Переменная, которая будет аккумулировать строку
                            StringBuilder msgString = new StringBuilder();

                            ch.pipeline().addLast(
                                    new ChannelInboundHandlerAdapter() {

                                        @Override
                                        public void channelRegistered(ChannelHandlerContext ctx) {
                                            System.out.println("Канал зарегистрирован");
                                        }

                                        @Override
                                        public void channelUnregistered(ChannelHandlerContext ctx) {
                                            System.out.println("Канал разрегистрирован");
                                        }

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) {
                                            System.out.println("Канал активирован");
                                        }

                                        @Override
                                        public void channelInactive(ChannelHandlerContext ctx) {
                                            System.out.println("Канал деактивирован");
                                        }

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                            System.out.println("Чтение из канала очередного символа");
                                            final ByteBuf m = (ByteBuf) msg;
                                            if (((char) m.readByte()) != '\n') {
                                                msgString.append((char) m.getByte(0));
                                            } else {
                                                System.out.println("Получена строка " + "[ " + msgString + " ]");
                                                ctx.writeAndFlush(Unpooled.wrappedBuffer(msgString.toString().getBytes(StandardCharsets.UTF_8)));
                                            }
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                            System.out.println("Cause exception");
                                            cause.printStackTrace();
                                            ctx.close(); //Инициируем отключение клиента
                                        }
                                    }
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Channel channel = server.bind(port).sync().channel();

            System.out.println("Server started");
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
