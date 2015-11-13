package se.crisp.codekvast.agent.lib.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

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
 * @author olle.hallin@crisp.se
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ComputerID {
    private final String value;

    @Override
    public String toString() {
        return value;
    }

    public static ComputerID compute() {
        String value = computeComputerIdentity();
        return new ComputerID(value);
    }

    private static String computeComputerIdentity() {
        Set<String> items = new TreeSet<String>();
        addMacAddresses(items);
        addHostName(items);
        return Integer.toHexString(items.hashCode()).toLowerCase();
    }

    private static void addHostName(Set<String> items) {
        try {
            items.add(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException ignore) {
        }
    }

    private static void addMacAddresses(Set<String> items) {
        try {
            for (Enumeration<NetworkInterface> it = NetworkInterface.getNetworkInterfaces(); it.hasMoreElements(); ) {
                NetworkInterface ni = it.nextElement();
                if (!ni.isLoopback() && !ni.getName().contains("vbox") && !ni.getName().contains("docker")) {
                    // TODO: Are there other strange interface names to avoid?
                    items.add(prettyPrintMacAddress(ni.getHardwareAddress()));
                }
            }
        } catch (SocketException ignore) {
            // Cannot enumerate network interfaces
        }
    }

    private static String prettyPrintMacAddress(byte[] macAddress) throws SocketException {
        StringBuilder sb = new StringBuilder();
        if (macAddress != null) {
            for (byte b : macAddress) {
                sb.append(String.format("%02x", b));
            }
        }
        return sb.toString();
    }
}
