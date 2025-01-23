package bgu.spl.net.impl.stomp;
import java.util.function.Supplier;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.*;

public class TpcServerImpl extends BaseServer<StompFrame>{
    public TpcServerImpl(
            int port,
            Supplier<MessagingProtocol<StompFrame>> protocolFactory,
            Supplier<MessageEncoderDecoder<StompFrame>> encdecFactory){
                super(port,protocolFactory,encdecFactory);
    }

    @Override
    protected void execute(BlockingConnectionHandler<StompFrame> handler) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }
    
}
