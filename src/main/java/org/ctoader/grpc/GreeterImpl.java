package org.ctoader.grpc;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;

public class GreeterImpl extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String message = new StringBuilder()
                .append("hello ")
                .append(request.getName())
                .toString();

        HelloReply reply = HelloReply.newBuilder()
                .setMessage(message)
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
