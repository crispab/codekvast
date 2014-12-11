package se.crisp.codekvast.agent.main;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is a computed value of the computer identity. It uses various stuff for computing the value, that is unlikely to change between
 * reboots.
 *
 * @author Olle Hallin
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ComputerID {
    private final String value;

    @Override
    public String toString() {
        return value;
    }

    public static ComputerID compute() {
        String value = computeComputerIdentity();
        log.info("Computed the computer ID '{}'", value);
        return new ComputerID(value);
    }

    private static String computeComputerIdentity() {
        Set<String> items = new TreeSet<>();
        addMacAddresses(items);
        addHostName(items);
        return Integer.toHexString(items.hashCode()).toLowerCase();
    }

    private static void addHostName(Set<String> items) {
        try {
            items.add(InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException e) {
            log.error("Cannot get name of localhost");
        }
    }

    private static void addMacAddresses(Set<String> items) {
        try {
            for (Enumeration<NetworkInterface> it = NetworkInterface.getNetworkInterfaces(); it.hasMoreElements(); ) {
                items.add(prettyPrintMacAddress(it.nextElement().getHardwareAddress()));
            }
        } catch (SocketException e) {
            log.error("Cannot enumerate network interfaces");
        }
    }

    private static String prettyPrintMacAddress(byte[] macAddress) throws SocketException {
        StringBuilder sb = new StringBuilder();
        if (macAddress != null) {
            String delimiter = "";
            for (byte b : macAddress) {
                sb.append(String.format("%s%02x", delimiter, b));
                delimiter = ":";
            }
        }
        return sb.toString();
    }
}
