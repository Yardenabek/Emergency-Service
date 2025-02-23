package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java StompServer <port> <tpc/reactor>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            String serverType = args[1].toLowerCase();

            switch (serverType) {
                case "tpc":
                    System.out.println("Initializing a TPC server");
                    Server.threadPerClient(
                            port, // The port
                            StompMessagingProtocolImpl::new, // Protocol factory
                            StompEncoderDecoder::new // Encoder-decoder factory
                    ).serve();
                    break;

                case "reactor":
                System.out.println("Initializing a Reactor server");
                    Server.reactor(
                            Runtime.getRuntime().availableProcessors(), // Number of threads
                            port, // The port
                            StompMessagingProtocolImpl::new, // Protocol factory
                            StompEncoderDecoder::new // Encoder-decoder factory
                    ).serve();
                    break;

                default:
                    System.err.println("Invalid server type. Use 'tpc' or 'reactor'.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number. Port must be an integer.");
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
