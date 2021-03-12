package org.ctoader.grpc;

import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class BenchmarkServer {

    private static final Logger logger = Logger.getLogger(BenchmarkServer.class.getName());

    private static final int port = 8080;

    private static final String certChainFilePath = "src/main/resources/server-cert.pem";
    private static final String privateKeyFilePath = "src/main/resources/server-key.pem";
//    private static final String trustCertCollectionFilePath = "todo";

    public static void main(String[] args) throws IOException, InterruptedException {

        ThreadFactory tf = new DefaultThreadFactory("server-elg-", true /*daemon */);

        final Server server = NettyServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .sslContext(getSslContextBuilder().build())
                .bossEventLoopGroup(new NioEventLoopGroup(1, tf))
                .workerEventLoopGroup(new NioEventLoopGroup(0, tf))
                .channelType(NioServerSocketChannel.class)
                .executor(new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                        new ForkJoinPool.ForkJoinWorkerThreadFactory() {
                            final AtomicInteger num = new AtomicInteger();

                            @Override
                            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                                ForkJoinWorkerThread thread =
                                        ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                                thread.setDaemon(true);
                                thread.setName("grpc-server-app-" + "-" + num.getAndIncrement());
                                return thread;
                            }
                        }, UncaughtExceptionHandlers.systemExit(), true /* async */))
                .build()
                .start();

        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                server.shutdown();
                System.err.println("*** server shut down");
            }
        });

        server.awaitTermination();
    }

    private static SslContextBuilder getSslContextBuilder() {
        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(certChainFilePath), new File(privateKeyFilePath));
//        sslClientContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
//        sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);

        return GrpcSslContexts.configure(sslClientContextBuilder);
    }
}
