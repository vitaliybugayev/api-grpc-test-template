package client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import utils.PropertiesManager;

import java.util.logging.Logger;

public class BaseGrpcClient {
    private static final Logger log = Logger.getLogger(BaseGrpcClient.class.getName());
    private ManagedChannel channel;

    /**
     * Constructor for service with direct host and port
     */
    public BaseGrpcClient(String host, int port) {
        setChannel(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext().build());
    }

    /**
     * Constructor using properties for service configuration
     */
    public BaseGrpcClient(String servicePropertyPrefix) {
        var envPrefix = servicePropertyPrefix.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        var envHostVar = envPrefix + "_HOST"; // e.g. USER_SERVICE_HOST
        var envPortVar = envPrefix + "_PORT"; // e.g. USER_SERVICE_PORT

        var propHostKey = servicePropertyPrefix + ".host";
        var propPortKey = servicePropertyPrefix + ".port";

        var host = System.getenv(envHostVar);
        var portStr = System.getenv(envPortVar);

        if (host == null || host.isEmpty()) {
            host = PropertiesManager.getProperty(propHostKey);
        }

        Integer port = null;
        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                log.warning("Invalid env port '" + envPortVar + "': " + portStr + ". Falling back to properties.");
            }
        }
        if (port == null) {
            try {
                port = Integer.parseInt(PropertiesManager.getProperty(propPortKey));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to resolve port from env '" + envPortVar + "' or property '" + propPortKey + "'", e);
            }
        }

        setChannel(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext().build());
    }

    /**
     * Constructor for localhost with specific port (for local development)
     */
    public BaseGrpcClient(int servicePort) {
        setChannel(channel(servicePort));
    }

    public static ManagedChannel channel(int servicePort) {
        return ManagedChannelBuilder
                .forAddress("localhost", servicePort)
                .usePlaintext()
                .build();
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void setChannel(ManagedChannel channel) {
        this.channel = channel;
    }

    public static Logger getLog() {
        return log;
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}
