package io.trik.capacitor.zeroconf;

import static android.content.Context.NSD_SERVICE;
import static android.content.Context.WIFI_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.getcapacitor.JSObject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ZeroConf {

    private static final String TAG = "ZeroConf";

    WifiManager.MulticastLock lock;
    private List<InetAddress> addresses;
    private List<InetAddress> ipv6Addresses;
    private List<InetAddress> ipv4Addresses;
    private String hostname;
    private RegistrationManager registrationManager;
    private BrowserManager browserManager;
    private Context context;
    private NsdManager nsdManager;
    private Handler mainHandler;

    public void initialize(Activity activity) {
        this.context = activity.getApplicationContext();
        this.nsdManager = (NsdManager) context.getSystemService(NSD_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        WifiManager wifi = (WifiManager) context.getSystemService(WIFI_SERVICE);
        lock = wifi.createMulticastLock("ZeroConfPluginLock");
        lock.setReferenceCounted(false);

        try {
            addresses = new CopyOnWriteArrayList<>();
            ipv6Addresses = new CopyOnWriteArrayList<>();
            ipv4Addresses = new CopyOnWriteArrayList<>();
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.supportsMulticast()) {
                    List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                    for (InetAddress address : addresses) {
                        if (!address.isLoopbackAddress()) {
                            if (address instanceof Inet6Address) {
                                this.addresses.add(address);
                                ipv6Addresses.add(address);
                            } else if (address instanceof Inet4Address) {
                                this.addresses.add(address);
                                ipv4Addresses.add(address);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        Log.d(TAG, "Addresses " + addresses);

        try {
            hostname = getHostNameFromActivity(activity);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        Log.d(TAG, "Hostname " + hostname);

        Log.v(TAG, "Initialized");
    }

    public String getHostname() {
        Log.d(TAG, "Hostname: " + hostname);
        return hostname;
    }

    public NsdServiceInfo registerService(String type, String domain, String name, int port, JSObject props,
            String addressFamily)
            throws RuntimeException {
        Log.d(TAG, "Register " + type + domain);
        if (registrationManager == null) {
            List<InetAddress> selectedAddresses = addresses;
            if ("ipv6".equalsIgnoreCase(addressFamily)) {
                selectedAddresses = ipv6Addresses;
            } else if ("ipv4".equalsIgnoreCase(addressFamily)) {
                selectedAddresses = ipv4Addresses;
            }
            registrationManager = new RegistrationManager(nsdManager, selectedAddresses, hostname);
        }

        NsdServiceInfo service = registrationManager.register(type, domain, name, port, props);
        if (service == null) {
            throw new RuntimeException("Failed to register");
        }
        return service;
    }

    public void unregisterService(String type, String domain, String name) {
        Log.d(TAG, "Unregister " + type + domain);

        if (registrationManager != null) {
            registrationManager.unregister(type, domain, name);
        }
    }

    public void stop() {
        Log.d(TAG, "Stop");

        final RegistrationManager rm = registrationManager;
        registrationManager = null;
        if (rm != null) {
            rm.stop();
        }
    }

    public void watchService(String type, String domain, String addressFamily, ZeroConfServiceWatchCallback callback)
            throws RuntimeException {
        Log.d(TAG, "Watch " + type + domain);

        if (browserManager == null) {
            List<InetAddress> selectedAddresses = addresses;
            if ("ipv6".equalsIgnoreCase(addressFamily)) {
                selectedAddresses = ipv6Addresses;
            } else if ("ipv4".equalsIgnoreCase(addressFamily)) {
                selectedAddresses = ipv4Addresses;
            }
            browserManager = new BrowserManager(nsdManager, selectedAddresses, hostname);
        }
        browserManager.watch(type, domain, callback);
    }

    public void unwatchService(String type, String domain) {
        Log.d(TAG, "Unwatch " + type + domain);
        if (browserManager != null) {
            browserManager.unwatch(type, domain);
        }
    }

    public void close() {
        Log.d(TAG, "Close");

        if (browserManager != null) {
            final BrowserManager bm = browserManager;
            browserManager = null;
            bm.close();
        }
    }

    private static class RegistrationManager {

        private final NsdManager nsdManager;
        private final Map<String, NsdServiceInfo> registeredServices = new HashMap<>();
        private final Map<String, NsdManager.RegistrationListener> registrationListeners = new HashMap<>();

        public RegistrationManager(NsdManager nsdManager, List<InetAddress> addresses, String hostname) {
            this.nsdManager = nsdManager;
        }

        public NsdServiceInfo register(String type, String domain, String name, int port, JSObject props) {
            String serviceKey = type + domain + name;

            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(name);
            serviceInfo.setServiceType(type);
            serviceInfo.setPort(port);

            // Add TXT records if provided
            if (props != null) {
                Map<String, String> txtRecord = new HashMap<>();
                Iterator<String> iterator = props.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String value = props.getString(key);
                    if (value != null) {
                        txtRecord.put(key, value);
                    }
                }

                // Convert map to attributes for NsdServiceInfo
                for (Map.Entry<String, String> entry : txtRecord.entrySet()) {
                    serviceInfo.setAttribute(entry.getKey(), entry.getValue());
                }
            }

            NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {
                @Override
                public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                    Log.d(TAG, "Service registered: " + nsdServiceInfo.getServiceName());
                    registeredServices.put(serviceKey, nsdServiceInfo);
                }

                @Override
                public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.e(TAG, "Service registration failed: " + serviceInfo.getServiceName() + " Error: " + errorCode);
                }

                @Override
                public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                    Log.d(TAG, "Service unregistered: " + nsdServiceInfo.getServiceName());
                    registeredServices.remove(serviceKey);
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.e(TAG,
                            "Service unregistration failed: " + serviceInfo.getServiceName() + " Error: " + errorCode);
                }
            };

            registrationListeners.put(serviceKey, registrationListener);
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);

            return serviceInfo;
        }

        public void unregister(String type, String domain, String name) {
            String serviceKey = type + domain + name;
            NsdManager.RegistrationListener listener = registrationListeners.get(serviceKey);
            if (listener != null) {
                nsdManager.unregisterService(listener);
                registrationListeners.remove(serviceKey);
            }
        }

        public void stop() {
            for (NsdManager.RegistrationListener listener : registrationListeners.values()) {
                try {
                    nsdManager.unregisterService(listener);
                } catch (Exception e) {
                    Log.e(TAG, "Error unregistering service", e);
                }
            }
            registrationListeners.clear();
            registeredServices.clear();
        }
    }

    private class BrowserManager {
        private String st;
        private final NsdManager nsdManager;
        private final Map<String, ZeroConfServiceWatchCallback> calls = new HashMap<>();
        private final Map<String, NsdManager.DiscoveryListener> discoveryListeners = new HashMap<>();

        public BrowserManager(NsdManager nsdManager, List<InetAddress> addresses, String hostname) {
            this.nsdManager = nsdManager;
            lock.acquire();
        }

        private void watch(String type, String domain, ZeroConfServiceWatchCallback callback) {
            String serviceKey = type + domain;
            Log.d(TAG, "watch record: " + serviceKey);

            calls.put(serviceKey, callback);

            NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
                @Override
                public void onDiscoveryStarted(String regType) {
                    Log.d(TAG, "Service discovery started for: " + regType);
                }

                @Override
                public void onServiceFound(NsdServiceInfo service) {
                    Log.d(TAG, "Service found: " + service.getServiceName());

                    // Resolve the service to get full details before sending callbacks
                    nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed for: " + serviceInfo.getServiceName() + " Error: " + errorCode);
                            // Still send the added callback even if resolve fails, but with limited info
                            sendCallback(ZeroConfServiceWatchCallback.ADDED, serviceInfo);
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.d(TAG, "Service resolved: " + serviceInfo.getServiceName() +
                                    ", Port: " + serviceInfo.getPort() +
                                    ", Host: "
                                    + (serviceInfo.getHost() != null ? serviceInfo.getHost().toString() : "null"));
                            // Send both ADDED and RESOLVED callbacks with the resolved service info
                            sendCallback(ZeroConfServiceWatchCallback.ADDED, serviceInfo);
                            sendCallback(ZeroConfServiceWatchCallback.RESOLVED, serviceInfo);
                        }
                    });
                }

                @Override
                public void onServiceLost(NsdServiceInfo service) {
                    Log.d(TAG, "Service lost: " + service.getServiceName());
                    sendCallback(ZeroConfServiceWatchCallback.REMOVED, service);
                }

                @Override
                public void onDiscoveryStopped(String serviceType) {
                    Log.d(TAG, "Discovery stopped for: " + serviceType);
                }

                @Override
                public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "Discovery failed for: " + serviceType + " Error: " + errorCode);
                }

                @Override
                public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "Stop discovery failed for: " + serviceType + " Error: " + errorCode);
                }
            };

            discoveryListeners.put(serviceKey, discoveryListener);
            nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }

        private void unwatch(String type, String domain) {
            String serviceKey = type + domain;
            calls.remove(serviceKey);

            NsdManager.DiscoveryListener listener = discoveryListeners.get(serviceKey);
            if (listener != null) {
                nsdManager.stopServiceDiscovery(listener);
                discoveryListeners.remove(serviceKey);
            }
        }

        private void close() {
            lock.release();
            calls.clear();

            for (NsdManager.DiscoveryListener listener : discoveryListeners.values()) {
                try {
                    nsdManager.stopServiceDiscovery(listener);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping service discovery", e);
                }
            }
            discoveryListeners.clear();
        }

        public void sendCallback(String action, NsdServiceInfo service) {
            String st = service.getServiceType().replaceAll("^\\.+|\\.+$", "") + ".local.";
            ZeroConfServiceWatchCallback callback = calls.get(st);
            if (callback == null) {
                Log.d(TAG, "sendCallback: no callback for " + st);
                return;
            }
            callback.serviceBrowserEvent(action, service);
        }
    }

    private static String getHostNameFromActivity(Activity activity)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        @SuppressLint("DiscouragedPrivateApi")
        Method getString = Build.class.getDeclaredMethod("getString", String.class);
        getString.setAccessible(true);
        String hostName = Objects.requireNonNull(getString.invoke(null, "net.hostname")).toString();

        // Fix for Bug https://github.com/becvert/cordova-plugin-zeroconf/issues/93
        // "unknown" seams a possible result since Android Oreo (8).
        // https://android-developers.googleblog.com/2017/04/changes-to-device-identifiers-in.html
        // Observed with: Android 8 on a Samsung S9,
        // Android 10 an 11 on a Samsung S10,
        // Android 11 on AVD Emulator

        if (TextUtils.isEmpty(hostName) || hostName.equals("unknown")) {
            // API 26+ :
            // Querying the net.hostname system property produces a null result
            @SuppressLint("HardwareIds")
            String id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
            hostName = "android-" + id;
        }
        return hostName;
    }
}
