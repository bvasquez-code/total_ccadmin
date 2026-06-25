package com.ccadmin.app.system.utility;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utilidad para obtener información de red del equipo local (IP/MAC/Hostname).
 * - Seguro ante nulls
 * - Evita loopback/virtual cuando se busca la "mejor" interfaz
 * - No usa librerías externas
 */
public final class NetInfoUtil {

    private NetInfoUtil() { }

    /* =========================
     * DTO simple
     * ========================= */
    public static final class NicInfo {
        private final String interfaceName;
        private final String displayName;
        private final String mac;
        private final List<String> ipv4;
        private final List<String> ipv6;
        private final boolean up;
        private final boolean loopback;
        private final boolean virtual;

        public NicInfo(String interfaceName,
                       String displayName,
                       String mac,
                       List<String> ipv4,
                       List<String> ipv6,
                       boolean up,
                       boolean loopback,
                       boolean virtual) {
            this.interfaceName = interfaceName;
            this.displayName = displayName;
            this.mac = mac;
            this.ipv4 = ipv4;
            this.ipv6 = ipv6;
            this.up = up;
            this.loopback = loopback;
            this.virtual = virtual;
        }

        public String getInterfaceName() { return interfaceName; }
        public String getDisplayName() { return displayName; }
        public String getMac() { return mac; }
        public List<String> getIpv4() { return ipv4; }
        public List<String> getIpv6() { return ipv6; }
        public boolean isUp() { return up; }
        public boolean isLoopback() { return loopback; }
        public boolean isVirtual() { return virtual; }
    }

    /* =========================
     * Host / Local basic
     * ========================= */

    /** Hostname local (puede fallar por DNS/hosts). */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Lista de todas las interfaces (incluye virtual/loopback). */
    public static List<NicInfo> getAllNics() throws SocketException {
        List<NicInfo> result = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

        while (ifaces.hasMoreElements()) {
            NetworkInterface nif = ifaces.nextElement();

            boolean up = safeIsUp(nif);
            boolean loopback = safeIsLoopback(nif);
            boolean virtual = nif.isVirtual();

            String mac = formatMac(safeHardwareAddress(nif));
            List<String> v4 = new ArrayList<>();
            List<String> v6 = new ArrayList<>();

            Enumeration<InetAddress> addrs = nif.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr instanceof Inet4Address) v4.add(addr.getHostAddress());
                else if (addr instanceof Inet6Address) v6.add(addr.getHostAddress());
            }

            result.add(new NicInfo(
                    nif.getName(),
                    nif.getDisplayName(),
                    mac,
                    v4,
                    v6,
                    up,
                    loopback,
                    virtual
            ));
        }

        return result;
    }

    /* =========================
     * "Best" IP/MAC (recomendado)
     * ========================= */

    /**
     * Devuelve la "mejor" IP local IPv4 (no loopback, interfaz UP, no virtual).
     * Si no encuentra, retorna null.
     */
    public static String getBestLocalIpv4() throws SocketException {
        BestPick pick = pickBestInterfaceAndIpv4();
        return pick == null ? null : pick.ipv4;
    }

    /**
     * Devuelve la MAC de la interfaz que corresponde a la "mejor" IP local IPv4.
     * Si no encuentra, retorna null.
     */
    public static String getBestLocalMac() throws SocketException {
        BestPick pick = pickBestInterfaceAndIpv4();
        return pick == null ? null : formatMac(safeHardwareAddress(pick.nif));
    }

    /**
     * Devuelve un NicInfo de la interfaz "mejor" (UP, no loopback, no virtual) que tenga IPv4.
     * Si no encuentra, retorna null.
     */
    public static NicInfo getBestNicInfo() throws SocketException {
        BestPick pick = pickBestInterfaceAndIpv4();
        if (pick == null) return null;

        NetworkInterface nif = pick.nif;
        List<String> v4 = new ArrayList<>();
        List<String> v6 = new ArrayList<>();

        Enumeration<InetAddress> addrs = nif.getInetAddresses();
        while (addrs.hasMoreElements()) {
            InetAddress addr = addrs.nextElement();
            if (addr instanceof Inet4Address) v4.add(addr.getHostAddress());
            else if (addr instanceof Inet6Address) v6.add(addr.getHostAddress());
        }

        return new NicInfo(
                nif.getName(),
                nif.getDisplayName(),
                formatMac(safeHardwareAddress(nif)),
                v4,
                v6,
                safeIsUp(nif),
                safeIsLoopback(nif),
                nif.isVirtual()
        );
    }

    /**
     * Obtiene la MAC a partir de una IP LOCAL del propio equipo.
     * (No sirve para IPs remotas en internet.)
     */
    public static String getMacFromLocalIp(String localIp) throws Exception {
        if (localIp == null || localIp.isBlank()) return null;

        InetAddress addr = InetAddress.getByName(localIp);
        NetworkInterface nif = NetworkInterface.getByInetAddress(addr);
        if (nif == null) return null;

        return formatMac(safeHardwareAddress(nif));
    }

    /* =========================
     * Helpers internos
     * ========================= */

    private static final class BestPick {
        private final NetworkInterface nif;
        private final String ipv4;

        private BestPick(NetworkInterface nif, String ipv4) {
            this.nif = nif;
            this.ipv4 = ipv4;
        }
    }

    private static BestPick pickBestInterfaceAndIpv4() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

        while (ifaces.hasMoreElements()) {
            NetworkInterface nif = ifaces.nextElement();

            if (!safeIsUp(nif)) continue;
            if (safeIsLoopback(nif)) continue;
            if (nif.isVirtual()) continue;

            Enumeration<InetAddress> addrs = nif.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    return new BestPick(nif, addr.getHostAddress());
                }
            }
        }
        return null;
    }

    private static boolean safeIsUp(NetworkInterface nif) {
        try { return nif.isUp(); } catch (Exception ignored) { return false; }
    }

    private static boolean safeIsLoopback(NetworkInterface nif) {
        try { return nif.isLoopback(); } catch (Exception ignored) { return false; }
    }

    private static byte[] safeHardwareAddress(NetworkInterface nif) {
        try { return nif.getHardwareAddress(); } catch (Exception ignored) { return null; }
    }

    /** MAC como AA-BB-CC-DD-EE-FF (si no disponible => null). */
    public static String formatMac(byte[] mac) {
        if (mac == null || mac.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
            if (i < mac.length - 1) sb.append("-");
        }
        return sb.toString();
    }
}
